package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.SensorStatisticsDto;
import com.example.springGroupBA.entity.sensor.Sensor;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.repository.sensor.SensorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SensorAnalysisService {

  private final SensorRepository sensorRepository;
  private final ThresholdService thresholdService;   // ⭐ DB 기반 임계치 적용

  // ==========================================================
  // ⭐ DB 임계치 기반 이상 여부 계산
  // ==========================================================
  private boolean isAbnormal(String device, SensorType type, Double value) {
    if (value == null) return false;

    SensorThreshold t = thresholdService.getThreshold(device, type);
    if (t == null) return false;  // 임계치 없는 타입은 이상 없음

    Double min = t.getMinValue();
    Double max = t.getMaxValue();

    if (min != null && value < min) return true;
    if (max != null && value > max) return true;

    return false;
  }


  // ==========================================================
  // 2) 통계 계산 (이 부분은 임계치와 무관 → 그대로 유지)
  // ==========================================================
  public SensorStatisticsDto calculateStats(String deviceCode) {
    Map<String, Object> row = sensorRepository.getStatistics(deviceCode);

    if (row == null || row.get("cnt") == null) {
      return emptyStatistics();
    }

    return SensorStatisticsDto.builder()
            // 환경
            .avgTemp(toDouble(row.get("avg_temp")))
            .minTemp(toDouble(row.get("min_temp")))
            .maxTemp(toDouble(row.get("max_temp")))
            .avgHumid(toDouble(row.get("avg_humid")))
            .minHumid(toDouble(row.get("min_humid")))
            .maxHumid(toDouble(row.get("max_humid")))
            .avgCo2(toDouble(row.get("avg_co2")))
            .minCo2(toDouble(row.get("min_co2")))
            .maxCo2(toDouble(row.get("max_co2")))
            .avgVoc(toDouble(row.get("avg_voc")))
            .minVoc(toDouble(row.get("min_voc")))
            .maxVoc(toDouble(row.get("max_voc")))

            // PM
            .avgPm1(toDouble(row.get("avg_pm1")))
            .minPm1(toDouble(row.get("min_pm1")))
            .maxPm1(toDouble(row.get("max_pm1")))
            .avgPm25(toDouble(row.get("avg_pm25")))
            .minPm25(toDouble(row.get("min_pm25")))
            .maxPm25(toDouble(row.get("max_pm25")))
            .avgPm10(toDouble(row.get("avg_pm10")))
            .minPm10(toDouble(row.get("min_pm10")))
            .maxPm10(toDouble(row.get("max_pm10")))

            // 온도 센서
            .avgT1(toDouble(row.get("avg_t1")))
            .minT1(toDouble(row.get("min_t1")))
            .maxT1(toDouble(row.get("max_t1")))
            .avgT2(toDouble(row.get("avg_t2")))
            .minT2(toDouble(row.get("min_t2")))
            .maxT2(toDouble(row.get("max_t2")))
            .avgT3(toDouble(row.get("avg_t3")))
            .minT3(toDouble(row.get("min_t3")))
            .maxT3(toDouble(row.get("max_t3")))
            .avgTnc(toDouble(row.get("avg_tnc")))
            .minTnc(toDouble(row.get("min_tnc")))
            .maxTnc(toDouble(row.get("max_tnc")))

            // noise
            .avgNoise(toDouble(row.get("avg_noise")))
            .minNoise(toDouble(row.get("min_noise")))
            .maxNoise(toDouble(row.get("max_noise")))

            // lux
            .avgLux(toDouble(row.get("avg_lux")))
            .minLux(toDouble(row.get("min_lux")))
            .maxLux(toDouble(row.get("max_lux")))

            .count(toLong(row.get("cnt")))
            .build();
  }


  private SensorStatisticsDto emptyStatistics() {
    return SensorStatisticsDto.builder()
            .avgTemp(0).minTemp(0).maxTemp(0)
            .avgHumid(0).minHumid(0).maxHumid(0)
            .avgCo2(0).minCo2(0).maxCo2(0)
            .avgVoc(0).minVoc(0).maxVoc(0)
            .avgPm1(0).minPm1(0).maxPm1(0)
            .avgPm25(0).minPm25(0).maxPm25(0)
            .avgPm10(0).minPm10(0).maxPm10(0)
            .avgT1(0).minT1(0).maxT1(0)
            .avgT2(0).minT2(0).maxT2(0)
            .avgT3(0).minT3(0).maxT3(0)
            .avgTnc(0).minTnc(0).maxTnc(0)
            .avgNoise(0).minNoise(0).maxNoise(0)
            .avgLux(0).minLux(0).maxLux(0)
            .count(0)
            .build();
  }

  private double toDouble(Object o) {
    if (o == null) return 0;
    try { return Double.parseDouble(o.toString()); }
    catch (Exception e) { return 0; }
  }

  private long toLong(Object o) {
    if (o == null) return 0;
    try {
      if (o instanceof Number n) return n.longValue();
      return Long.parseLong(o.toString());
    } catch (Exception e) {
      return 0;
    }
  }
}
