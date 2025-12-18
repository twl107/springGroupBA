package com.example.springGroupBA.entity.sensor;

import com.example.springGroupBA.dto.sensor.SensorInputDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sensor")
public class Sensor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "sensor_id")
  private Long sensorId;

  @Column(name = "company_id")
  private Integer companyId;

  @Column(name = "device_code", length = 20)
  private String deviceCode;

  @Column(name = "measure_datetime")
  private LocalDateTime measureDatetime;

  // ===== CSV 기반 value_1 ~ value_20 =====

  @Column(name = "value_1")
  private Double value1;

  @Column(name = "value_2")
  private Double value2;

  @Column(name = "value_3")
  private Double value3;

  @Column(name = "value_4")
  private Double value4;

  @Column(name = "value_5")
  private Double value5;

  @Column(name = "value_6")
  private Double value6;

  @Column(name = "value_7")
  private Double value7;

  @Column(name = "value_8")
  private Double value8;

  @Column(name = "value_9")
  private Double value9;

  @Column(name = "value_10")
  private Double value10;

  @Column(name = "value_11")
  private Double value11;

  @Column(name = "value_12")
  private Double value12;

  @Column(name = "value_13")
  private Double value13;

  @Column(name = "value_14")
  private Double value14;

  @Column(name = "value_15")
  private Double value15;

  @Column(name = "value_16")
  private Double value16;

  @Column(name = "value_17")
  private Double value17;

  @Column(name = "value_18")
  private Double value18;

  @Column(name = "value_19")
  private Double value19;

  @Column(name = "value_20")
  private Double value20;

  public static Sensor dtoToEntity(SensorInputDto dto) {
    return Sensor.builder()
            .companyId(dto.getCompanyId())
            .deviceCode(dto.getDeviceCode())
            .measureDatetime(parseDate(dto.getMeasureDatetime()))
            .value1(parseToDouble(dto.getValue1()))
            .value2(parseToDouble(dto.getValue2()))
            .value3(parseToDouble(dto.getValue3()))
            .value4(parseToDouble(dto.getValue4()))
            .value5(parseToDouble(dto.getValue5()))
            .value6(parseToDouble(dto.getValue6()))
            .value7(parseToDouble(dto.getValue7()))
            .value8(parseToDouble(dto.getValue8()))
            .value9(parseToDouble(dto.getValue9()))
            .value10(parseToDouble(dto.getValue10()))
            .value11(parseToDouble(dto.getValue11()))
            .value12(parseToDouble(dto.getValue12()))
            .value13(parseToDouble(dto.getValue13()))
            .value14(parseToDouble(dto.getValue14()))
            .value15(parseToDouble(dto.getValue15()))
            .value16(parseToDouble(dto.getValue16()))
            .value17(parseToDouble(dto.getValue17()))
            .value18(parseToDouble(dto.getValue18()))
            .value19(parseToDouble(dto.getValue19()))
            .value20(parseToDouble(dto.getValue20()))
            .build();
  }

  private static Double parseToDouble(String v) {
    if (v == null) return null;

    v = v.trim();      // 공백 제거
    if (v.isEmpty()) return null;
    if (v.equalsIgnoreCase("null")) return null;

    try {
      return Double.parseDouble(v);
    } catch (Exception e) {
      return null;
    }
  }

  private static LocalDateTime parseDate(String v) {
    if (v == null) return null;

    v = v.trim();
    if (v.isEmpty()) return null;

    // CSV 날짜 패턴 후보들
    String[] patterns = {
            "yyyy-MM-dd HH:mm:ss",  // ✔ CSV 기본 형식
            "yyyy-MM-dd HH:mm",     // 서브 패턴
            "yyyy/MM/dd HH:mm:ss",  // 혹시 / 사용하는 경우 대비
            "yyyy/MM/dd HH:mm"
    };

    for (String p : patterns) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(p);
        return LocalDateTime.parse(v, formatter);
      } catch (Exception ignore) {}
    }

    // 모든 포맷 실패 시 NULL
    return null;
  }

}
