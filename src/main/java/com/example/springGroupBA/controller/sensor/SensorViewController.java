package com.example.springGroupBA.controller.sensor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SensorViewController { // html을 보여주는 단순 view 처리

  @GetMapping("/sensor/dashboard")
  public String dashboard(
          @RequestParam(name = "deviceCode", defaultValue = "ENV_V2_1") String deviceCode,
          Model model
  ) {
    model.addAttribute("deviceCode", deviceCode);
    return "sensor/dashboard"; // templates/sensor/dashboard.html
  }

  @GetMapping("/sensor/report")
  public String reportPage() {

    return "sensor/report"; // /templates/sensor/report.html
  }

  @GetMapping("/sensor/dailyReport")
  public String dailyReportPage(
          @RequestParam(name = "deviceCode", defaultValue = "ENV_V2_1") String deviceCode,
          Model model
  ) {
    model.addAttribute("deviceCode", deviceCode);
    return "sensor/dailyReport"; // templates/sensor/dailyReport.html
  }
}
