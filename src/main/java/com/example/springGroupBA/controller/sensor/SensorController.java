package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.dto.sensor.SensorRealtimeDto;
import com.example.springGroupBA.dto.sensor.SensorStatisticsDto;
import com.example.springGroupBA.service.sensor.SensorAnalysisService;
import com.example.springGroupBA.service.sensor.SensorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorController {

  private final SensorService sensorService;
  private final SensorAnalysisService sensorAnalysisService;

  /** 공통 날짜 포맷 */
  private static final DateTimeFormatter FMT =
          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private LocalDateTime parseDateTime(String s) {
    return LocalDateTime.parse(s, FMT);
  }

  /* ========================================================
   *  1. 실시간 / 최근 데이터 조회
   * ======================================================== */

  @GetMapping("/latest")
  public SensorRealtimeDto getLatest(@RequestParam String deviceCode) {
    return sensorService.getLatestData(deviceCode)
            .map(SensorRealtimeDto::entityToDto)
            .orElse(null);
  }

  @GetMapping("/recent")
  public List<SensorRealtimeDto> getRecent(
          @RequestParam String deviceCode,
          @RequestParam(defaultValue = "100") int limit
  ) {
    return sensorService.getRecentData(deviceCode, limit)
            .stream()
            .map(SensorRealtimeDto::entityToDto)
            .toList();
  }

  /* ========================================================
   *  2. REALTIME 보조용 기간 조회 (range)
   *    - 현재 JS에서는 사용하지 않지만, 유효한 API
   * ======================================================== */

  @GetMapping("/range")
  public List<SensorRealtimeDto> getRange(
          @RequestParam String deviceCode,
          @RequestParam String start,
          @RequestParam String end
  ) {
    LocalDateTime s = parseDateTime(start);
    LocalDateTime e = parseDateTime(end);

    return sensorService.getDataByPeriod(deviceCode, s, e)
            .stream()
            .map(SensorRealtimeDto::entityToDto)
            .toList();
  }

  /* ========================================================
   *  3. 회사 데이터 조회
   * ======================================================== */

  @GetMapping("/company/{companyId}")
  public List<SensorRealtimeDto> getCompanyData(@PathVariable Integer companyId) {
    return sensorService.getCompanyData(companyId)
            .stream()
            .map(SensorRealtimeDto::entityToDto)
            .toList();
  }

  /* ========================================================
   *  4. ⭐ HISTORY 조회
   *     - 단일 센서 기준
   * ======================================================== */

  @GetMapping("/history")
  public List<Map<String, Object>> getHistory(
          @RequestParam String deviceCode,
          @RequestParam String sensorType,
          @RequestParam String start,
          @RequestParam String end
  ) {
    LocalDateTime s = parseDateTime(start);
    LocalDateTime e = parseDateTime(end);

    return sensorService.getHistory(deviceCode, sensorType, s, e);
  }

  /* ========================================================
   *  5. CSV 시뮬레이터 CHUNK 로드
   * ======================================================== */

  @PostMapping("/load-chunk")
  public Map<String, Object> loadChunk(
          @RequestParam(defaultValue = "200") int size
  ) {
    int loaded = sensorService.loadNextChunk(size);
    boolean done = (loaded == 0);

    return Map.of(
            "loaded", loaded,
            "offset", SensorService.CSV_OFFSET,
            "done", done
    );
  }

  /* ========================================================
   *  6. ⭐ REALTIME 시작일 재설정
   * ======================================================== */

  @PostMapping("/set-start-date")
  public Map<String, Object> setStartDate(@RequestParam String startDate) {

    LocalDateTime parsed = parseDateTime(startDate);

    sensorService.resetDatabase();
    sensorService.resetCsvState(parsed);

    log.info("CSV 시작일 변경됨: {}", parsed);

    return Map.of(
            "startDate", parsed.toString(),
            "offset", SensorService.CSV_OFFSET
    );
  }

  /* ========================================================
   *  7. 시뮬레이터 제어
   * ======================================================== */

  @PostMapping("/pause")
  public void pause() {
    SensorService.SIMULATION_PAUSED = true;
    log.info("=== SIMULATION PAUSED ===");
  }

  @PostMapping("/resume")
  public void resume() {
    SensorService.SIMULATION_PAUSED = false;
    log.info("=== SIMULATION RESUMED ===");
  }

  @PostMapping("/reset")
  public void reset() {

    SensorService.SIMULATION_PAUSED = true;
    SensorService.CSV_OFFSET = 0;
    SensorService.CSV_START_DATE = null;

    sensorService.resetDatabase();

    log.info("=== SENSOR SYSTEM RESET: CSV_OFFSET=0, DB cleared ===");
  }

  /* ========================================================
   *  8. 통계 조회
   * ======================================================== */

  @GetMapping("/stats")
  public SensorStatisticsDto getStatistics(@RequestParam String deviceCode) {
    return sensorAnalysisService.calculateStats(deviceCode);
  }
}
