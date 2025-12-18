package com.example.springGroupBA.entity.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sensor_report")
public class SensorReport {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "device_code", length = 20, nullable = false)
  private String deviceCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "sensor_type", length = 20, nullable = false)
  private SensorType sensorType;

  @Column(nullable = false)
  private Integer year;

  @Column(nullable = false)
  private Integer month;

  @Column
  private Integer week;
  // 주차 통계는 week = 1~5, 월 통계는 week = null

  @Column(name = "min_value")
  private Double minValue;

  @Column(name = "avg_value")
  private Double avgValue;

  @Column(name = "max_value")
  private Double maxValue;

  @Column(name = "bad_count")
  private Long badCount;

  @Column(name = "sample_count")
  private Long sampleCount;

  /* ⭐ 추가: 이벤트 발생 건수 */
  @Column(name = "event_count")
  private Long eventCount;

}
