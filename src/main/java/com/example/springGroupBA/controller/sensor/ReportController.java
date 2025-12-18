package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.SensorEventDto;
import com.example.springGroupBA.service.sensor.ReportService;
import com.example.springGroupBA.service.sensor.SensorEventService;
import com.example.springGroupBA.service.sensor.SensorRawLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/report")
public class ReportController {

  private final ReportService reportService;
  private final SensorRawLoadService sensorRawLoadService;
  private final SensorEventService sensorEventService;

  /** CSV → sensor_raw 테이블 로드 */
  @PostMapping("/load-csv")
  public String loadCsv() {
    sensorRawLoadService.loadCsv();
    return "OK";
  }

  /** sensor_raw → sensor_report 월 처리 */
  @PostMapping("/process/{year}/{month}")
  public String process(
          @PathVariable int year,
          @PathVariable int month,
          @RequestParam String device
  ) {
    reportService.processMonth(year, month, device);
    return "OK";
  }

  /** 리포트 조회 */
  @GetMapping("/{year}/{month}")
  public Object getReport(
          @PathVariable int year,
          @PathVariable int month,
          @RequestParam String device
  ) {
    return reportService.getReport(year, month, device);
  }

  @GetMapping("/events")
  public List<SensorEventDto> getEvents(
          @RequestParam String scope,       // MONTH or WEEK
          @RequestParam int year,
          @RequestParam int month,
          @RequestParam String device,
          @RequestParam String sensorType,
          @RequestParam(required = false) Integer week
  ) {
    log.info("API EVENT LOG → scope={}, year={}, month={}, week={}, device={}, sensorType={}",
            scope, year, month, week, device, sensorType);

    return sensorEventService.getEventLogs(
            scope,
            year,
            month,
            week,
            device,
            sensorType     // SensorType이 아닌 String!
    );
  }
}
