package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.entity.sensor.SensorEvent;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SensorEventRepository extends JpaRepository<SensorEvent, Long> {

  // (월/주) 이벤트 카운트
  long countByDeviceCodeAndSensorTypeAndStartDatetimeBetween(
          String deviceCode,
          SensorType sensorType,
          LocalDateTime start,
          LocalDateTime end
  );

  // 이벤트 조회
  List<SensorEvent> findByDeviceCodeAndSensorTypeAndStartDatetimeBetweenOrderByStartDatetimeAsc(
          String deviceCode,
          SensorType sensorType,
          LocalDateTime start,
          LocalDateTime end
  );

  // 기존 월 데이터 삭제
  @Transactional
  void deleteByDeviceCodeAndSensorTypeAndStartDatetimeBetween(
          String deviceCode,
          SensorType sensorType,
          LocalDateTime start,
          LocalDateTime end
  );

  @Query("""
            SELECT COUNT(e)
            FROM SensorEvent e
            WHERE e.deviceCode = :deviceCode
              AND e.sensorType = :sensorType
              AND e.startDatetime < :end
              AND (e.endDatetime >= :start OR e.endDatetime IS NULL)
          """)
  long countEventsOverlapping(
          @Param("deviceCode") String deviceCode,
          @Param("sensorType") SensorType sensorType,
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end
  );

}
