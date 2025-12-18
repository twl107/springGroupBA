package com.example.springGroupBA.entity.sensor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Immutable   // 읽기 전용!
@Table(name = "sensor_raw")
public class SensorRaw {

  @Id
  @Column(name = "sensor_id")
  private Long sensorId;

  @Column(name = "company_id")
  private Integer companyId;

  @Column(name = "device_code")
  private String deviceCode;

  @Column(name = "measure_datetime")
  private LocalDateTime measureDatetime;

  @Column(name = "value_1")
  private Double value1;

  @Column(name = "value_2")
  private Double value2;

  @Column(name = "value_3")
  private Double value3;

  @Column(name = "value_4")
  private Double value4;

  @Column(name = "value_5")
  private Double value5;

  @Column(name = "value_6")
  private Double value6;

  @Column(name = "value_7")
  private Double value7;

  @Column(name = "value_8")
  private Double value8;

  @Column(name = "value_9")
  private Double value9;

  @Column(name = "value_10")
  private Double value10;

  @Column(name = "value_11")
  private Double value11;

  @Column(name = "value_12")
  private Double value12;

  @Column(name = "value_13")
  private Double value13;

  @Column(name = "value_14")
  private Double value14;

  @Column(name = "value_15")
  private Double value15;

  @Column(name = "value_16")
  private Double value16;

  @Column(name = "value_17")
  private Double value17;

  @Column(name = "value_18")
  private Double value18;

  @Column(name = "value_19")
  private Double value19;

  @Column(name = "value_20")
  private Double value20;
}
