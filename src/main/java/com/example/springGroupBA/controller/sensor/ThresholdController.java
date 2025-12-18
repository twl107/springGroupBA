package com.example.springGroupBA.controller.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.ThresholdDto;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.service.sensor.ThresholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/threshold")
public class ThresholdController {

  private final ThresholdService thresholdService;

  /**
   * GET: threshold 조회
   */
  @GetMapping("/get")
  public ThresholdDto getThreshold(
          @RequestParam String device,
          @RequestParam int index
  ) {
    SensorType type = SensorType.fromFieldIndex(index + 1); // front에서 index는 0부터

    SensorThreshold t = thresholdService.getThreshold(device, type);

    if (t == null) {
      // 기본값 제공
      return ThresholdDto.builder()
              .deviceCode(device)
              .sensorType(type)
              .minValue(null)
              .maxValue(null)
              .active("Y")
              .build();
    }

    return ThresholdDto.builder()
            .deviceCode(device)
            .sensorType(type)
            .minValue(t.getMinValue())
            .maxValue(t.getMaxValue())
            .active(t.getActive())
            .build();
  }

  /**
   * POST: threshold 저장
   */
  @PostMapping("/save")
  public String saveThreshold(@RequestBody ThresholdDto dto) {

    // sensorType 비어있으면 sensorIndex로부터 자동 매핑
    if (dto.getSensorType() == null) {

      if (dto.getSensorIndex() == null) {
        return "ERROR: sensorIndex is null";
      }

      SensorType type = SensorType.fromFieldIndex(dto.getSensorIndex() + 1);
      dto.setSensorType(type);
    }

    if (dto.getSensorType() == null) {
      return "ERROR: invalid sensorIndex = " + dto.getSensorIndex();
    }

    // active 기본값 보정
    if (dto.getActive() == null || dto.getActive().isBlank()) {
      dto.setActive("Y");
    }

    thresholdService.saveThreshold(dto);
    return "OK";
  }
}
