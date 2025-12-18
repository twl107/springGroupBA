package com.example.springGroupBA.dto.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThresholdDto {

  // 🔹 프론트에서 오는 인덱스 (0 ~ 12)
  private Integer sensorIndex;

  private String deviceCode;
  private SensorType sensorType;

  private Double minValue;
  private Double maxValue;

  // "Y" / "N"
  private String active;
}
