package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.entity.sensor.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SensorRepository extends JpaRepository<Sensor, Long> {

  //  최신 1건
  Optional<Sensor> findTopByDeviceCodeOrderByMeasureDatetimeDesc(String deviceCode);

  //  최근 N건 (Pageable 기반)
  Page<Sensor> findByDeviceCodeOrderByMeasureDatetimeDesc(String deviceCode, Pageable pageable);

  //  회사 전체 데이터 조회
  List<Sensor> findByCompanyId(Integer companyId);

  //  특정 기간 조회
  List<Sensor> findByDeviceCodeAndMeasureDatetimeBetween(
          String deviceCode,
          LocalDateTime start,
          LocalDateTime end
  );

  // dash board pause시 통계 계산 native sql
  @Query(value = """
            SELECT 
              COUNT(*) AS cnt,
          
              AVG(value_1) AS avg_temp,
              MIN(value_1) AS min_temp,
              MAX(value_1) AS max_temp,
          
              AVG(value_2) AS avg_humid,
              MIN(value_2) AS min_humid,
              MAX(value_2) AS max_humid,
          
              AVG(value_3) AS avg_co2,
              MAX(value_3) AS max_co2,
          
              AVG(value_4) AS avg_voc,
          
              AVG(value_5) AS avg_pm1,
              MIN(value_5) AS min_pm1,
              MAX(value_5) AS max_pm1,
          
              AVG(value_6) AS avg_pm25,
              MIN(value_6) AS min_pm25,
              MAX(value_6) AS max_pm25,
          
              AVG(value_7) AS avg_pm10,
              MIN(value_7) AS min_pm10,
              MAX(value_7) AS max_pm10,
          
              AVG(value_8) AS avg_t1,
              MIN(value_8) AS min_t1,
              MAX(value_8) AS max_t1,
          
              AVG(value_9) AS avg_t2,
              MIN(value_9) AS min_t2,
              MAX(value_9) AS max_t2,
          
              AVG(value_10) AS avg_t3,
              MIN(value_10) AS min_t3,
              MAX(value_10) AS max_t3,
          
              AVG(value_11) AS avg_noise,
              MAX(value_11) AS max_noise,
          
              AVG(value_12) AS avg_tnc,
              MIN(value_12) AS min_tnc,
              MAX(value_12) AS max_tnc,
          
              AVG(value_13) AS avg_lux,
              MAX(value_13) AS max_lux
          
          FROM sensor
          WHERE device_code = :deviceCode
          """, nativeQuery = true)
  Map<String, Object> getStatistics(@Param("deviceCode") String deviceCode);

}
