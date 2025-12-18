package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.SensorEventDto;
import com.example.springGroupBA.entity.sensor.SensorEvent;
import com.example.springGroupBA.entity.sensor.SensorRaw;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.repository.sensor.SensorEventRepository;
import com.example.springGroupBA.repository.sensor.SensorRawRepository;
import com.example.springGroupBA.repository.sensor.SensorThresholdRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorEventService {

  private final SensorRawRepository rawRepository;
  private final SensorThresholdRepository thresholdRepository;
  private final SensorEventRepository eventRepository;

  // =====================================================================
  // A안 : RAW 스캔 → 이벤트 생성(open/close) → 월 단위 DB 저장
  // =====================================================================
  @Transactional
  public List<SensorEventDto> analyzeRange(
          String deviceCode,
          String sensorTypeStr,
          LocalDateTime start,
          LocalDateTime end,
          boolean persist,
          LocalDateTime deleteStart,
          LocalDateTime deleteEnd
  ) {

    SensorType sensorType = SensorType.fromAny(sensorTypeStr);
    if (sensorType == null) {
      log.warn("❗ SensorType 변환 실패: {}", sensorTypeStr);
      return List.of();
    }

    SensorThreshold threshold = thresholdRepository
            .findByDeviceCodeAndSensorType(deviceCode, sensorType)
            .orElse(null);

    if (threshold == null) {
      log.warn("⚠ threshold 없음 → device={} type={}", deviceCode, sensorType);
      return List.of();
    }

    // ------------------------------------------------------------
    // persist = true → 기존 월 이벤트 삭제 (월 누적 저장)
    // ------------------------------------------------------------
    if (persist) {
      eventRepository.deleteByDeviceCodeAndSensorTypeAndStartDatetimeBetween(
              deviceCode, sensorType, deleteStart, deleteEnd
      );
    }

    // ------------------------------------------------------------
    // RAW 조회
    // ------------------------------------------------------------
    List<SensorRaw> raws = rawRepository
            .findByDeviceCodeAndMeasureDatetimeBetweenOrderByMeasureDatetime(
                    deviceCode, start, end
            );

    if (raws.isEmpty()) return List.of();

    List<SensorEventDto> result = new ArrayList<>();

    boolean inEvent = false;
    LocalDateTime evStart = null;
    Long rawStartId = null;
    String evLevel = null;

    // ------------------------------------------------------------
    // 스캔
    // ------------------------------------------------------------
    for (SensorRaw r : raws) {

      Double val = sensorType.getValue(r);
      if (val == null) continue;

      boolean isBad =
              (threshold.getMinValue() != null && val < threshold.getMinValue()) ||
                      (threshold.getMaxValue() != null && val > threshold.getMaxValue());

      if (isBad) {
        if (!inEvent) {
          inEvent = true;
          evStart = r.getMeasureDatetime();
          rawStartId = r.getSensorId();
          evLevel = determineLevel(threshold, val);
        }
      } else {
        // 이벤트 종료
        if (inEvent) {
          SensorEventDto dto = new SensorEventDto(
                  null,
                  deviceCode,
                  sensorType,
                  evStart,
                  r.getMeasureDatetime(),
                  evLevel,
                  "임계치 이탈",
                  rawStartId
          );
          result.add(dto);
          inEvent = false;
        }
      }
    }

    // 마지막 RAW까지 BAD였다면 close 처리
    if (inEvent) {
      SensorEventDto dto = new SensorEventDto(
              null,
              deviceCode,
              sensorType,
              evStart,
              null,
              evLevel,
              "임계치 이탈",
              rawStartId
      );
      result.add(dto);
    }

    // ------------------------------------------------------------
    // persist = true → 실제 DB 저장
    // ------------------------------------------------------------
    if (persist && !result.isEmpty()) {

      List<SensorEvent> entities = result.stream()
              .map(dto -> {
                SensorEvent e = new SensorEvent();
                e.setDeviceCode(deviceCode);
                e.setSensorType(sensorType);
                e.setStartDatetime(dto.getStartDatetime());
                e.setEndDatetime(dto.getEndDatetime());
                e.setEventLevel(dto.getEventLevel());
                e.setMessage(dto.getMessage());
                e.setRawId(dto.getRawId());
                return e;
              })
              .toList();

      eventRepository.saveAll(entities);
    }

    return result;
  }

  // =====================================================================
  // 레벨 계산
  // =====================================================================
  private String determineLevel(SensorThreshold th, Double value) {
    if (th.getMaxValue() != null && value > th.getMaxValue()) return "HIGH";
    if (th.getMinValue() != null && value < th.getMinValue()) return "LOW";
    return "BAD";
  }

  // =====================================================================
  // 이벤트 로그 조회 (scope = MONTH / WEEK)
  // =====================================================================
  public List<SensorEventDto> getEventLogs(
          String scope,
          int year,
          int month,
          Integer week,
          String deviceCode,
          String sensorTypeStr
  ) {

    SensorType sensorType = SensorType.fromAny(sensorTypeStr);
    if (sensorType == null) return List.of();

    LocalDateTime start;
    LocalDateTime end;

    // ------------------------------------------------------------
    // MONTH
    // ------------------------------------------------------------
    if ("MONTH".equalsIgnoreCase(scope)) {
      start = LocalDateTime.of(year, month, 1, 0, 0);
      end = start.plusMonths(1);
    }
    // ------------------------------------------------------------
    // WEEK
    // ------------------------------------------------------------
    else {
      if (week == null) return List.of();
      start = LocalDateTime.of(year, month, 1, 0, 0)
              .plusDays((week - 1) * 7);
      end = start.plusDays(7);
    }

    return eventRepository
            .findByDeviceCodeAndSensorTypeAndStartDatetimeBetweenOrderByStartDatetimeAsc(
                    deviceCode, sensorType, start, end
            )
            .stream()
            .map(this::toDto)
            .toList();
  }

  // =====================================================================
  // DTO 변환
  // =====================================================================
  private SensorEventDto toDto(SensorEvent e) {
    return new SensorEventDto(
            e.getEventId(),
            e.getDeviceCode(),
            e.getSensorType(),
            e.getStartDatetime(),
            e.getEndDatetime(),
            e.getEventLevel(),
            e.getMessage(),
            e.getRawId()
    );
  }
}
