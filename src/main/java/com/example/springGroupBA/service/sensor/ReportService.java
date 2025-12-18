package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.DeviceStatDto;
import com.example.springGroupBA.dto.sensor.SensorReportDto;
import com.example.springGroupBA.entity.sensor.SensorReport;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.repository.sensor.SensorEventRepository;
import com.example.springGroupBA.repository.sensor.SensorReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

  private final SensorReportRepository sensorReportRepository;
  private final JdbcTemplate jdbc;
  private final ThresholdService thresholdService;
  private final SensorEventRepository eventRepository;
  private final SensorEventService eventService;

  // SensorType → sensor_raw 컬럼 매핑
  private static final Map<SensorType, String> FIELD_MAP = new EnumMap<>(SensorType.class);

  static {
    FIELD_MAP.put(SensorType.TEMP, "value_1");
    FIELD_MAP.put(SensorType.HUMIDITY, "value_2");
    FIELD_MAP.put(SensorType.CO2, "value_3");
    FIELD_MAP.put(SensorType.VOC, "value_4");
    FIELD_MAP.put(SensorType.PM1, "value_5");
    FIELD_MAP.put(SensorType.PM25, "value_6");
    FIELD_MAP.put(SensorType.PM10, "value_7");
    FIELD_MAP.put(SensorType.T1, "value_8");
    FIELD_MAP.put(SensorType.T2, "value_9");
    FIELD_MAP.put(SensorType.T3, "value_10");
    FIELD_MAP.put(SensorType.NOISE, "value_11");
    FIELD_MAP.put(SensorType.TNC, "value_12");
    FIELD_MAP.put(SensorType.LUX, "value_13");
    // value_14 ~ value_20 을 쓰게 되면 여기 계속 매핑 추가
    /*FIELD_MAP.put(SensorType.VAL14, "value_14");
    FIELD_MAP.put(SensorType.VAL15, "value_15");
    FIELD_MAP.put(SensorType.VAL16, "value_16");
    FIELD_MAP.put(SensorType.VAL17, "value_17");
    FIELD_MAP.put(SensorType.VAL18, "value_18");
    FIELD_MAP.put(SensorType.VAL19, "value_19");
    FIELD_MAP.put(SensorType.VAL20, "value_20");*/
  }

  // ============================================================
  // 주차 계산 (1 ~ 5주)
  // ============================================================
  private int getWeek(LocalDateTime ts) {
    LocalDate date = ts.toLocalDate();
    LocalDate firstDay = date.withDayOfMonth(1);
    int offset = firstDay.getDayOfWeek().getValue() - 1; // 월(1)~일(7) → 0~6
    int day = date.getDayOfMonth();
    // (일 + offset - 1) / 7 + 1  → 1~5주
    return Math.min(5, Math.max(1, (day + offset - 1) / 7 + 1));
  }

  // ============================================================
  // 월 보고서 생성 (raw → 이벤트 분석 → 통계 분석)
  // ============================================================
  public void processMonth(int year, int month, String deviceCode) {

    // 1) 기존 보고서 삭제
    sensorReportRepository.deleteByYearAndMonthAndDeviceCode(year, month, deviceCode);

    // 2) 기간 계산
    LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
    LocalDateTime monthEnd = monthStart.plusMonths(1);

    log.info("📆 REPORT 월 재계산 시작: {}-{} / device={}", year, month, deviceCode);

    // ------------------------------------------------------------
    // 3) 월간 이벤트 생성 (월 전체 구간)
    //    - persist = true
    //    - deleteStart/deleteEnd = 월 전체 구간
    // ------------------------------------------------------------
    for (SensorType type : FIELD_MAP.keySet()) {
      eventService.analyzeRange(
              deviceCode,
              type.name(),
              monthStart,
              monthEnd,
              true,
              monthStart,
              monthEnd
      );
    }

    // ------------------------------------------------------------
    // 4) RAW 데이터 조회 (해당 device, 해당 월)
    // ------------------------------------------------------------
    String sql = """
                SELECT measure_datetime,
                       value_1, value_2, value_3, value_4, value_5, value_6, value_7,
                       value_8, value_9, value_10, value_11, value_12, value_13
                FROM sensor_raw
                WHERE device_code = ?
                  AND measure_datetime >= ?
                  AND measure_datetime < ?
                """;

    List<Map<String, Object>> rows = jdbc.queryForList(sql, deviceCode, monthStart, monthEnd);

    if (rows.isEmpty()) {
      log.info("📂 RAW 데이터 없음: {}년 {}월 / device={}", year, month, deviceCode);
      return;
    }

    // ------------------------------------------------------------
    // 5) 통계 계산용 Map 준비
    // ------------------------------------------------------------
    Map<SensorType, List<Double>> monthMap = initEmptyTypeMap();
    Map<Integer, Map<SensorType, List<Double>>> weeklyMap = new HashMap<>();

    // ------------------------------------------------------------
    // 6) RAW 스캔 → 월간/주간 통계 분배
    // ------------------------------------------------------------
    for (Map<String, Object> row : rows) {

      LocalDateTime ts;
      Object tsObj = row.get("measure_datetime");
      if (tsObj instanceof java.sql.Timestamp t) {
        ts = t.toLocalDateTime();
      } else if (tsObj instanceof LocalDateTime ldt) {
        ts = ldt;
      } else {
        continue;
      }

      int week = getWeek(ts);
      weeklyMap.putIfAbsent(week, initEmptyTypeMap());

      for (SensorType type : FIELD_MAP.keySet()) {
        String col = FIELD_MAP.get(type);
        Object v = row.get(col);
        if (v == null) continue;

        double value = ((Number) v).doubleValue();
        monthMap.get(type).add(value);
        weeklyMap.get(week).get(type).add(value);
      }
    }

    List<SensorReport> batch = new ArrayList<>();

    // ------------------------------------------------------------
    // 7) 월 통계 + 이벤트 카운트 저장
    // ------------------------------------------------------------
    for (SensorType type : FIELD_MAP.keySet()) {
      List<Double> values = monthMap.get(type);
      addStatsRowWithEvent(
              batch,
              deviceCode,
              type,
              year,
              month,
              null,          // week=null → 월 통계
              values,
              monthStart,
              monthEnd
      );
    }

    // ------------------------------------------------------------
    // 8) 주간 통계 + (필요 시) 주간 이벤트 생성 + 이벤트 카운트 저장
    // ------------------------------------------------------------
    for (Map.Entry<Integer, Map<SensorType, List<Double>>> entry : weeklyMap.entrySet()) {
      Integer week = entry.getKey();
      Map<SensorType, List<Double>> weekMap = entry.getValue();

      // 주차 구간 대략 계산 (1주=7일 기준)
      LocalDateTime wStart = monthStart.plusDays((week - 1) * 7L);
      LocalDateTime wEnd = wStart.plusDays(7L);

      // 주간 이벤트 생성 (원하면 주간 이벤트도 따로 보고 가능)
      for (SensorType type : FIELD_MAP.keySet()) {
        eventService.analyzeRange(
                deviceCode,
                type.name(),
                wStart,
                wEnd,
                true,
                wStart,
                wEnd
        );
      }

      for (SensorType type : FIELD_MAP.keySet()) {
        List<Double> values = weekMap.get(type);
        addStatsRowWithEvent(
                batch,
                deviceCode,
                type,
                year,
                month,
                week,       // week != null → 주간 통계
                values,
                wStart,
                wEnd
        );
      }
    }

    // ------------------------------------------------------------
    // 9) DB 저장
    // ------------------------------------------------------------
    if (!batch.isEmpty()) {
      sensorReportRepository.saveAll(batch);
      log.info("✅ REPORT 저장 완료 - {} rows (eventCount 포함) / {}-{} / device={}",
              batch.size(), year, month, deviceCode);
    } else {
      log.info("⚠ REPORT에 저장할 데이터가 없습니다. {}-{} / device={}", year, month, deviceCode);
    }
  }

  // ============================================================
  // 단일 row 생성 + badCount + eventCount 계산
  // ============================================================
  private void addStatsRowWithEvent(
          List<SensorReport> batch,
          String deviceCode,
          SensorType type,
          int year,
          int month,
          Integer week,
          List<Double> values,
          LocalDateTime start,
          LocalDateTime end
  ) {
    if (values == null || values.isEmpty()) return;

    double min = values.stream().min(Double::compare).orElse(0.0);
    double max = values.stream().max(Double::compare).orElse(0.0);
    double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

    long sampleCount = values.size();

    // ------------------------------
    // BAD 계산 (기존 정책 그대로)
    // ------------------------------
    long badCount = 0;
    SensorThreshold th = thresholdService.getThreshold(deviceCode, type);
    if (th != null) {
      Double minL = th.getMinValue();
      Double maxL = th.getMaxValue();

      badCount = values.stream()
              .filter(v ->
                      (minL != null && v < minL) ||
                              (maxL != null && v > maxL)
              )
              .count();
    }

    // BAD%
    double badPercent = sampleCount > 0
            ? (badCount * 100.0 / sampleCount)
            : 0.0;

    // ------------------------------
    // EVENT (최신 SensorEventService 기반)
    // ------------------------------
    long eventCount =
            eventRepository.countEventsOverlapping(
                    deviceCode, type, start, end
            );

    // 저장
    SensorReport report = SensorReport.builder()
            .deviceCode(deviceCode)
            .sensorType(type)
            .year(year)
            .month(month)
            .week(week)
            .minValue(min)
            .avgValue(avg)
            .maxValue(max)
            .sampleCount(sampleCount)
            .badCount(badCount)
            .eventCount(eventCount)
            .build();

    batch.add(report);
  }


  private Map<SensorType, List<Double>> initEmptyTypeMap() {
    Map<SensorType, List<Double>> map = new EnumMap<>(SensorType.class);
    for (SensorType t : SensorType.values()) {
      map.put(t, new ArrayList<>());
    }
    return map;
  }

  // ============================================================
  // 리포트 조회 (없으면 자동 생성)
  // ============================================================
  public SensorReportDto getReport(int year, int month, String deviceCode) {

    List<SensorReport> rows =
            sensorReportRepository.findByYearAndMonthAndDeviceCode(year, month, deviceCode);

    // 보고서가 없으면 생성 후 다시 조회
    if (rows.isEmpty()) {
      log.info("📊 REPORT 미존재 → 생성 시도: {}-{} / device={}", year, month, deviceCode);
      processMonth(year, month, deviceCode);
      rows = sensorReportRepository.findByYearAndMonthAndDeviceCode(year, month, deviceCode);
    }

    List<DeviceStatDto> monthly = new ArrayList<>();
    Map<Integer, List<DeviceStatDto>> weekly = new TreeMap<>();

    for (SensorReport r : rows) {

      DeviceStatDto dto = DeviceStatDto.from(r);

      if (r.getWeek() == null) {
        monthly.add(dto);
      } else {
        weekly.computeIfAbsent(r.getWeek(), k -> new ArrayList<>())
                .add(dto);
      }
    }

    // SensorType 기준 정렬
    Comparator<DeviceStatDto> sortByType =
            Comparator.comparing(DeviceStatDto::getSensorType);

    monthly.sort(sortByType);
    weekly.values().forEach(list -> list.sort(sortByType));

    return SensorReportDto.builder()
            .year(year)
            .month(month)
            .deviceCode(deviceCode)
            .monthlyStats(monthly)
            .weeklyStats(weekly)
            .build();
  }

  private double nvl(Double v) {
    return v == null ? 0.0 : v;
  }

  private long nvlLong(Long v) {
    return v == null ? 0L : v;
  }
}
