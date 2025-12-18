package com.example.springGroupBA.dto.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.entity.sensor.SensorReport;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceStatDto {

  private String deviceCode;
  private SensorType sensorType; // TEMP, HUMIDITY, CO2, VOC, PM1, PM25, PM10, T1, T2, T3, NOISE, TNC, LUX
  private String label;

  private double minValue;
  private double avgValue;
  private double maxValue;

  private long badCount;
  private long sampleCount;

  // ⭐ 보고서 통합 eventCount
  private long eventCount;

  // ⭐ BAD% 추가
  private Double badRate;

  private Integer year;
  private Integer month;
  private Integer week;  // 주 단위는 주차, 월 단위는 null

  public static DeviceStatDto from(SensorReport r) {
    if (r == null) return null;

    long sample = r.getSampleCount() == null ? 0 : r.getSampleCount();
    long bad = r.getBadCount() == null ? 0 : r.getBadCount();
    Double badRate = (sample > 0) ? (bad * 100.0 / sample) : null;

    return DeviceStatDto.builder()
            .deviceCode(r.getDeviceCode())
            .sensorType(r.getSensorType())
            .label(r.getSensorType().getLabel())
            .minValue(r.getMinValue() == null ? 0.0 : r.getMinValue())
            .avgValue(r.getAvgValue() == null ? 0.0 : r.getAvgValue())
            .maxValue(r.getMaxValue() == null ? 0.0 : r.getMaxValue())
            .badCount(bad)
            .sampleCount(sample)
            .eventCount(r.getEventCount() == null ? 0 : r.getEventCount())
            .year(r.getYear())
            .month(r.getMonth())
            .week(r.getWeek())
            .badRate(badRate)
            .build();
  }
}
