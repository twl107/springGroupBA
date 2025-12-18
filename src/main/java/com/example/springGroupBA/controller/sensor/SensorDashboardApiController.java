package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.service.sensor.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class SensorDashboardApiController {

  private final ThresholdService thresholdService;

  /**
   * 활성 센서 목록 조회
   * - return 예: ["TEMP","CO2","PM25"]
   */
  @GetMapping("/active-sensors")
  public List<String> getActiveSensors(@RequestParam String deviceCode) {
    return thresholdService.getActiveSensorTypeNames(deviceCode);
  }

  /**
   * 특정 센서 threshold 조회
   * - return 예: { "min": 400, "max": 2000 }
   */
  @GetMapping("/threshold")
  public Map<String, Object> getThreshold(
          @RequestParam String deviceCode,
          @RequestParam String sensorType
  ) {

    SensorType type = SensorType.valueOf(sensorType);

    SensorThreshold t = thresholdService.getThreshold(deviceCode, type);

    Map<String, Object> result = new HashMap<>();

    if (t == null) {
      result.put("min", null);
      result.put("max", null);
      result.put("active", "Y");
    } else {
      result.put("min", t.getMinValue());
      result.put("max", t.getMaxValue());
      result.put("active", t.getActive());
    }

    return result;
  }

  /**
   * 활성 threshold 전체 조회
   */
  @GetMapping("/threshold/all")
  public List<SensorThreshold> getActiveThresholds(@RequestParam String deviceCode) {
    return thresholdService.getActiveThresholds(deviceCode);
  }

}
