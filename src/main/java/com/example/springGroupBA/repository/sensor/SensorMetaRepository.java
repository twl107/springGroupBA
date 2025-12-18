package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.entity.sensor.SensorMeta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorMetaRepository extends JpaRepository<SensorMeta, Long> {
  SensorMeta findByCode(String code);
}
