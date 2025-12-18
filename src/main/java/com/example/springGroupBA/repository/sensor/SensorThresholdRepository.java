package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SensorThresholdRepository
        extends JpaRepository<SensorThreshold, Long> {

  // 특정 센서 threshold 조회
  Optional<SensorThreshold> findByDeviceCodeAndSensorType(
          String deviceCode, SensorType sensorType
  );

  // ACTIVE 센서들만 가져오기
  List<SensorThreshold> findByDeviceCodeAndActive(String deviceCode, String active);

}
