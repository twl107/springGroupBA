package com.example.springGroupBA.entity.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sensor_threshold")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorThreshold {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long thresholdId;

  @Column(nullable = false, length = 20)
  private String deviceCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SensorType sensorType;

  /**
   * 최솟값 / 최댓값 (기준 임계값)
   * warnMin, warnMax 삭제 → 단일 min/max로 단순화
   */
  private Double minValue;
  private Double maxValue;

  /** 센서 활성 여부 (Y/N) */
  @Builder.Default
  @Column(nullable = false, length = 1, columnDefinition = "VARCHAR(1) DEFAULT 'Y'")
  private String active = "Y";

  /** 선택적 센서 위치 정보 */
  @Column(name = "location")
  private String location;
}
