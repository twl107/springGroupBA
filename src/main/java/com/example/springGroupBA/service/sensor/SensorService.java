package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.SensorInputDto;
import com.example.springGroupBA.dto.sensor.SensorRealtimeDto;
import com.example.springGroupBA.entity.sensor.Sensor;
import com.example.springGroupBA.entity.sensor.SensorRaw;
import com.example.springGroupBA.repository.sensor.SensorRawRepository;
import com.example.springGroupBA.repository.sensor.SensorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorService {

  private final SensorRepository sensorRepository;

  private final SensorRawRepository sensorRawRepository;

  private final SimpMessagingTemplate messagingTemplate;

  public static boolean SIMULATION_PAUSED = false;

  /** application.yml 에서 외부 파일 경로 주입 */
  @Value("${sensor.csv-path}")
  private String csvPath;

  /** 현재 CSV 오프셋 (헤더 제외 라인 수) */
  public static int CSV_OFFSET = 0;

  /** 유저가 설정한 시작 날짜 (OFFSET 계산 시에만 1회 사용) */
  public static LocalDateTime CSV_START_DATE = null;

  private static final DateTimeFormatter CSV_DTF =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // ===================================================================
  // 0. 시작 시 CSV 날짜 정보 찍어봄
  // ===================================================================
  @PostConstruct
  public void init() {
    debugCsvDateRange();
  }

  // ===================================================================
  // 1. 날짜 선택 시 OFFSET 1회 계산
  // ===================================================================
  public synchronized void resetCsvState(LocalDateTime startDate) {
    CSV_START_DATE = startDate;
    CSV_OFFSET = findOffsetByDate(startDate);
    log.info("CSV 초기화 완료 - START={}, OFFSET={}", startDate, CSV_OFFSET);
  }

  /**
   * startDate 이상의 첫 라인을 찾아 offset 계산
   */
  private int findOffsetByDate(LocalDateTime startDate) {
    Path path = Paths.get(csvPath);
    int offset = 0;

    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

      String line = br.readLine(); // header skip
      if (line == null) return 0;

      while ((line = br.readLine()) != null) {
        String[] arr = line.replace("\"", "").split(",");
        if (arr.length < 4) {
          offset++;
          continue;
        }

        LocalDateTime rowTime = parseDate(arr[3]);
        if (rowTime != null && !rowTime.isBefore(startDate)) {
          break;
        }
        offset++;
      }

    } catch (IOException e) {
      log.error("OFFSET 탐색 실패: {}", e.getMessage());
      return 0;
    }

    return offset;
  }

  // ===================================================================
  // 2. CSV chunk 로딩
  // ===================================================================
  @Transactional
  public synchronized int loadNextChunk(int chunkSize) {

    if (SIMULATION_PAUSED) {
      return 0; // 중단 상태면 CSV 읽지 않음
    }

    Path path = Paths.get(csvPath);
    List<Sensor> batch = new ArrayList<>();
    int readCount = 0;

    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

      // 1) 헤더 1줄 스킵
      br.readLine();

      // 2) OFFSET 만큼 MOVE (seek)
      String line;
      for (int i = 0; i < CSV_OFFSET; i++) {
        if (br.readLine() == null) {
          return 0; // EOF
        }
      }

      // 3) chunkSize 만큼 읽어서 DB 저장
      while (readCount < chunkSize && (line = br.readLine()) != null) {

        String raw = line.replace("\"", "").trim();
        String[] arr = raw.split(",");

        if (arr.length < 4) {
          CSV_OFFSET++;
          continue;
        }

        // 날짜 필터는 offset 계산에서만 사용. chunk 로딩에서는 제거!
        LocalDateTime ts = parseDate(arr[3]);

        CSV_OFFSET++;

        SensorInputDto dto = new SensorInputDto();
        dto.setCompanyId(parseInt(arr[1]));
        dto.setDeviceCode(arr[2]);
        dto.setMeasureDatetime(arr[3]);

        for (int i = 1; i <= 20; i++) {
          if (3 + i < arr.length)
            setDtoValue(dto, i, arr[3 + i]);
        }

        Sensor entity = Sensor.dtoToEntity(dto);
        batch.add(entity);

        // WebSocket
        messagingTemplate.convertAndSend(
                "/topic/sensor/" + entity.getDeviceCode(),
                SensorRealtimeDto.entityToDto(entity)
        );

        readCount++;
      }

    } catch (IOException e) {
      log.error("CSV chunk 로딩 오류: {}", e.getMessage());
    }

    if (!batch.isEmpty()) {
      sensorRepository.saveAll(batch);
    }

    return readCount;
  }

  // ===================================================================
  // 3. 조회 기능
  // ===================================================================
  public Optional<Sensor> getLatestData(String deviceCode) {
    return sensorRepository.findTopByDeviceCodeOrderByMeasureDatetimeDesc(deviceCode);
  }

  public List<Sensor> getRecentData(String deviceCode, int limit) {
    return sensorRepository
            .findByDeviceCodeOrderByMeasureDatetimeDesc(
                    deviceCode,
                    PageRequest.of(0, limit)
            ).getContent();
  }

  // ===================================================================
  // HISTORY MODE(DB 조회)
  // ===================================================================
  // ===================================================================
// HISTORY MODE - PK 기반 샘플링 버전 (권장, 고성능)
// ===================================================================
  public List<Map<String, Object>> getHistory(
          String deviceCode,
          String sensorType,
          LocalDateTime start,
          LocalDateTime end
  ) {

    SensorType type = SensorType.fromAny(sensorType);
    if (type == null) {
      throw new IllegalArgumentException("Unknown sensorType: " + sensorType);
    }

    // 1) 조회 구간 전체 카운트
    long totalCount = sensorRawRepository.countByDeviceCodeAndMeasureDatetimeBetween(
            deviceCode, start, end
    );

    if (totalCount == 0) return List.of();

    // 2) PK 샘플링 step 계산
    int maxPoints = 100;
    long step = (long) Math.ceil(totalCount / (double) maxPoints);

    // 3) PK 기반 샘플링 쿼리 실행
    List<SensorRaw> rows = sensorRawRepository.findSampledByDeviceCodeAndDatetime(
            deviceCode, start, end, step
    );

    // 4) SensorRaw → Map 변환
    List<Map<String, Object>> result = new ArrayList<>();
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    for (SensorRaw row : rows) {
      Double value = type.getValue(row);
      if (value == null) continue;

      Map<String, Object> map = new HashMap<>();
      map.put("timestamp", row.getMeasureDatetime().format(fmt));
      map.put("value", value);

      result.add(map);
    }

    return result;
  }



  // ===================================================================
  // UTIL
  // ===================================================================
  private LocalDateTime parseDate(String v) {
    try {
      return LocalDateTime.parse(v, CSV_DTF);
    } catch (Exception e) {
      return null;
    }
  }

  private Integer parseInt(String v) {
    try {
      return Integer.parseInt(v.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private void setDtoValue(SensorInputDto dto, int idx, String val) {
    switch (idx) {
      case 1 -> dto.setValue1(val);
      case 2 -> dto.setValue2(val);
      case 3 -> dto.setValue3(val);
      case 4 -> dto.setValue4(val);
      case 5 -> dto.setValue5(val);
      case 6 -> dto.setValue6(val);
      case 7 -> dto.setValue7(val);
      case 8 -> dto.setValue8(val);
      case 9 -> dto.setValue9(val);
      case 10 -> dto.setValue10(val);
      case 11 -> dto.setValue11(val);
      case 12 -> dto.setValue12(val);
      case 13 -> dto.setValue13(val);
      case 14 -> dto.setValue14(val);
      case 15 -> dto.setValue15(val);
      case 16 -> dto.setValue16(val);
      case 17 -> dto.setValue17(val);
      case 18 -> dto.setValue18(val);
      case 19 -> dto.setValue19(val);
      case 20 -> dto.setValue20(val);
    }
  }

  // ===================================================================
  // 디버그: CSV 앞뒤 날짜 확인
  // ===================================================================
  public void debugCsvDateRange() {
    try {
      Path path = Paths.get(csvPath);
      BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8);

      log.info("===== CSV 시작 부분 =====");
      for (int i = 0; i < 5; i++) {
        String line = br.readLine();
        if (line == null) break;
        log.info("{}", line.replace("\"", "").split(",")[3]);
      }

      List<String> tail = new ArrayList<>();
      String tmp;
      while ((tmp = br.readLine()) != null) {
        tail.add(tmp);
        if (tail.size() > 5) tail.remove(0);
      }

      log.info("===== CSV 마지막 부분 =====");
      for (String s : tail) {
        log.info("{}", s.replace("\"", "").split(",")[3]);
      }

    } catch (Exception ignore) {}
  }

  @Transactional
  public void resetDatabase() {
    sensorRepository.deleteAllInBatch();
    CSV_OFFSET = 0;
  }

  public List<Sensor> getDataByPeriod(String deviceCode, LocalDateTime start, LocalDateTime end) {
    return sensorRepository.findByDeviceCodeAndMeasureDatetimeBetween(
            deviceCode, start, end
    );
  }

  public List<Sensor> getCompanyData(Integer companyId) {
    return sensorRepository.findByCompanyId(companyId);
  }
  
  
}
