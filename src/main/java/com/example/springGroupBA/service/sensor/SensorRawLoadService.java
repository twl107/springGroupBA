package com.example.springGroupBA.service.sensor;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

@Service
@RequiredArgsConstructor
public class SensorRawLoadService {

  private final DataSource dataSource;

  @Value("${sensor.csv-path}")
  private String csvPath;

  // MySQL의 “LOAD DATA LOCAL INFILE” 기반 대량 데이터 로드
  public void loadCsv() {

    String sql = """
                LOAD DATA LOCAL INFILE '%s'
                INTO TABLE sensor_raw
                FIELDS TERMINATED BY ','
                OPTIONALLY ENCLOSED BY '"'
                IGNORE 1 LINES
                (sensor_id, company_id, device_code, measure_datetime,
                 value_1, value_2, value_3, value_4, value_5,
                 value_6, value_7, value_8, value_9, value_10,
                 value_11, value_12, value_13, value_14, value_15,
                 value_16, value_17, value_18, value_19, value_20)
            """.formatted(csvPath);

    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.execute();
    } catch (Exception e) {
      throw new RuntimeException("CSV 로드 중 오류 발생", e);
    }
  }
}
