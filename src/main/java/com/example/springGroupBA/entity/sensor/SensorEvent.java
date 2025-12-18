package com.example.springGroupBA.entity.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensorEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long eventId;

  @Column(nullable = false, length = 20)
  private String deviceCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SensorType sensorType;

  @Column(nullable = false, length = 10)
  private String eventLevel;

  @Column(nullable = false)
  private LocalDateTime startDatetime;

  private LocalDateTime endDatetime;

  @Column(length = 255)
  private String message;

  @Column(name = "raw_id", nullable = false)
  private Long rawId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) createdAt = LocalDateTime.now();
  }
}
