package com.example.springGroupBA.repository.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.entity.sensor.SensorReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SensorReportRepository extends JpaRepository<SensorReport, Long> {

  List<SensorReport> findByDeviceCodeAndYearAndMonth(String deviceCode, int year, int month);

  List<SensorReport> findByDeviceCodeAndYearAndMonthAndWeek(
          String deviceCode, int year, int month, int week);

  List<SensorReport> findByYearAndMonth(int year, int month);

  List<SensorReport> findByYearAndMonthAndWeek(int year, int month, int week);

  List<SensorReport> findBySensorTypeAndYearAndMonth(SensorType type, int year, int month);

  List<SensorReport> findByYearAndMonthAndDeviceCode(Integer year,
                                                     Integer month,
                                                     String deviceCode);

  void deleteByYearAndMonthAndDeviceCode(Integer year,
                                         Integer month,
                                         String deviceCode);


}
