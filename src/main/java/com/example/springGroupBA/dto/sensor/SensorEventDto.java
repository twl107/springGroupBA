package com.example.springGroupBA.dto.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorEventDto {

  private Long eventId;
  private String deviceCode;
  private SensorType sensorType;

  private LocalDateTime startDatetime;
  private LocalDateTime endDatetime;

  private String eventLevel;
  private String message;

  private Long rawId;
}
