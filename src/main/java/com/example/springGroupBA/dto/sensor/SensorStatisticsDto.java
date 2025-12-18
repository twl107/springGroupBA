package com.example.springGroupBA.dto.sensor;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SensorStatisticsDto {

  // 환경 센서
  private double avgTemp;
  private double minTemp;
  private double maxTemp;

  private double avgHumid;
  private double minHumid;
  private double maxHumid;

  private double avgCo2;
  private double minCo2;
  private double maxCo2;

  private double avgVoc;
  private double minVoc;
  private double maxVoc;

  // PM
  private double avgPm1;
  private double minPm1;
  private double maxPm1;

  private double avgPm25;
  private double minPm25;
  private double maxPm25;

  private double avgPm10;
  private double minPm10;
  private double maxPm10;

  // 온도 센서군
  private double avgT1;
  private double minT1;
  private double maxT1;

  private double avgT2;
  private double minT2;
  private double maxT2;

  private double avgT3;
  private double minT3;
  private double maxT3;

  private double avgTnc;
  private double minTnc;
  private double maxTnc;

  // Noise
  private double avgNoise;
  private double minNoise;
  private double maxNoise;

  // Lux
  private double avgLux;
  private double minLux;
  private double maxLux;

  // 전체 데이터 개수
  private long count;
}


