package com.example.springGroupBA.dto.sensor;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class SensorReportDto {

  private int year;
  private int month;
  private String deviceCode;
  private List<DeviceStatDto> monthlyStats;
  private Map<Integer, List<DeviceStatDto>> weeklyStats;
}
