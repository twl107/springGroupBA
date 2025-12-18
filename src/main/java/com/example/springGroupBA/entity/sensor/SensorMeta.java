package com.example.springGroupBA.entity.sensor;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sensor_meta")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class SensorMeta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 센서 구분 코드 (iaq, pt100, noise, ir, lux ...)
  @Column(unique = true, nullable = false)
  private String code;

  // 센서명
  @Column(nullable = false)
  private String name;

  // 모델명 (ST-IAQ-07 등)
  private String model;

  // 카드에 표시될 간단 설명
  private String summary;

  // 상세 설명
  @Column(columnDefinition = "TEXT")
  private String description;

  // (선택) 카드 이미지
  private String imageUrl;
}