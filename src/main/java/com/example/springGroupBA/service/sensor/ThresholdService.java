package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.ThresholdDto;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.repository.sensor.SensorThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThresholdService {

  private final SensorThresholdRepository repo;

  /** SensorType + deviceCode 기준으로 Threshold 조회 */
  public SensorThreshold getThreshold(String deviceCode, SensorType type) {
    return repo.findByDeviceCodeAndSensorType(deviceCode, type)
            .orElse(null);
  }

  /** DTO → Entity 저장 (신규 or 업데이트) */
  public SensorThreshold saveThreshold(ThresholdDto dto) {

    SensorThreshold t = repo.findByDeviceCodeAndSensorType(dto.getDeviceCode(), dto.getSensorType())
            .orElseGet(() -> SensorThreshold.builder()
                    .deviceCode(dto.getDeviceCode())
                    .sensorType(dto.getSensorType())
                    .build()
            );

    t.setMinValue(dto.getMinValue());
    t.setMaxValue(dto.getMaxValue());
    t.setActive(dto.getActive());

    return repo.save(t);
  }

  /** min/max 기준 초과 여부 */
  public boolean isOutOfRange(String deviceCode, SensorType type, Double value) {
    SensorThreshold t = getThreshold(deviceCode, type);
    if (t == null || value == null) return false;

    if (t.getMinValue() != null && value < t.getMinValue()) return true;
    if (t.getMaxValue() != null && value > t.getMaxValue()) return true;

    return false;
  }

    /* ============================================================
         🔥 여기서부터 ACTIVE/THRESHOLD “목록 조회” 기능 추가
       ============================================================ */

  /** deviceCode 기준 “활성화된 센서(sensor_type)” 목록 반환 */
  public List<SensorType> getActiveSensorTypes(String deviceCode) {
    return repo.findByDeviceCodeAndActive(deviceCode, "Y")
            .stream()
            .map(SensorThreshold::getSensorType)
            .toList();
  }

  /** 활성화된 센서 목록 (문자열 형태로 반환 — 프론트에서 사용) */
  public List<String> getActiveSensorTypeNames(String deviceCode) {
    return repo.findByDeviceCodeAndActive(deviceCode, "Y")
            .stream()
            .map(t -> t.getSensorType().name())
            .toList();
  }

  /** deviceCode 기준 활성 threshold 전체 조회 */
  public List<SensorThreshold> getActiveThresholds(String deviceCode) {
    return repo.findByDeviceCodeAndActive(deviceCode, "Y");
  }

  /** 전체 threshold 조회 (활성/비활성 모두 포함) */
  public List<SensorThreshold> getAllThresholds(String deviceCode) {
    return repo.findByDeviceCodeAndActive(deviceCode, "Y");
    // 필요하면 active 제거하고 다른 메서드 작성
  }

}
