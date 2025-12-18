package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.entity.sensor.SensorRaw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorRawRepository extends JpaRepository<SensorRaw, Long> {

  // 1) HISTORY 기간 조회 (Dashboard HISTORY / Report용)
  List<SensorRaw> findByDeviceCodeAndMeasureDatetimeBetween(
          String deviceCode,
          LocalDateTime start,
          LocalDateTime end
  );

  // 2) 기존의 "가장 가까운 1개" 조회
  @Query("""
              SELECT s
              FROM SensorRaw s
              WHERE s.deviceCode = :device
                AND s.measureDatetime BETWEEN :start AND :end
              ORDER BY ABS(TIMESTAMPDIFF(SECOND, s.measureDatetime, :target))
          """)
  List<SensorRaw> findClosestRecord(
          @Param("device") String device,
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("target") LocalDateTime target
  );

  @Query(value =
          "SELECT COUNT(*) FROM sensor_raw " +
                  "WHERE device_code = :deviceCode " +
                  "AND measure_datetime BETWEEN :start AND :end",
          nativeQuery = true)
  long countByDeviceCodeAndMeasureDatetimeBetween(
          String deviceCode,
          LocalDateTime start,
          LocalDateTime end
  );

  @Query(value =
          "SELECT * FROM sensor_raw " +
                  "WHERE device_code = :deviceCode " +
                  "AND measure_datetime BETWEEN :start AND :end " +
                  "AND MOD(sensor_id, :step) = 0 " +
                  "ORDER BY sensor_id",
          nativeQuery = true)
  List<SensorRaw> findSampledByDeviceCodeAndDatetime(
          String deviceCode,
          LocalDateTime start,
          LocalDateTime end,
          long step
  );

  //  이벤트 분석용 — 정렬된 RAW 조회
  List<SensorRaw> findByDeviceCodeAndMeasureDatetimeBetweenOrderByMeasureDatetime(
          String deviceCode,
          LocalDateTime start,
          LocalDateTime end
  );

  // target 이전 가장 최신 raw (prev)
  @Query("""
            SELECT s
            FROM SensorRaw s
            WHERE s.deviceCode = :device
              AND s.measureDatetime <= :target
            ORDER BY s.measureDatetime DESC
          """)
  List<SensorRaw> findPrevRaw(
          @Param("device") String device,
          @Param("target") LocalDateTime target
  );

  // target 이후 가장 빠른 raw (next)
  @Query("""
            SELECT s
            FROM SensorRaw s
            WHERE s.deviceCode = :device
              AND s.measureDatetime >= :target
            ORDER BY s.measureDatetime ASC
          """)
  List<SensorRaw> findNextRaw(
          @Param("device") String device,
          @Param("target") LocalDateTime target
  );


}

