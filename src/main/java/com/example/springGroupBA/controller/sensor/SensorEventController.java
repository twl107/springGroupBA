package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.dto.sensor.SensorEventDto;
import com.example.springGroupBA.service.sensor.SensorEventService;
import com.example.springGroupBA.service.sensor.ExcelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SensorEventController {

  private final SensorEventService eventService;
  private final ExcelService excelService;

  // --------------------------
  // SSR 화면
  // --------------------------
  @GetMapping("/sensor/event-log")
  public String eventLogView(
          @RequestParam String scope,
          @RequestParam int year,
          @RequestParam int month,
          @RequestParam String device,
          @RequestParam String sensorType,
          @RequestParam(required = false) Integer week,
          Model model
  ) {
    model.addAttribute("scope", scope);
    model.addAttribute("year", year);
    model.addAttribute("month", month);
    model.addAttribute("device", device);
    model.addAttribute("sensorType", sensorType);
    model.addAttribute("week", week);

    return "sensor/eventLog";   // eventLog.html
  }

  /**
   * Excel 다운로드
   */
  @GetMapping("/sensor/event-log/download")
  public void downloadEventLogExcel(
          @RequestParam String scope,
          @RequestParam int year,
          @RequestParam int month,
          @RequestParam String device,
          @RequestParam String sensorType,
          @RequestParam(required = false) Integer week,
          HttpServletResponse response
  ) throws Exception {

    List<SensorEventDto> logs =
            eventService.getEventLogs(scope, year, month, week, device, sensorType);

    String filename = String.format("event-log-%s-%d-%02d-%s-%s.xlsx",
            scope.toLowerCase(), year, month, device, sensorType);

    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename=" + filename);

    excelService.writeEventLogExcel(logs, response.getOutputStream());
  }
}
