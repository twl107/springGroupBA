package com.example.springGroupBA.dto.sensor;

import com.example.springGroupBA.entity.sensor.Sensor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SensorRealtimeDto {

  private String deviceCode;
  private String timestamp;

  private Double temp;            // value1
  private Double humidity;        // value2
  private Double co2;             // value3
  private Double voc;             // value4
  private Double pm1;             // value5
  private Double pm25;            // value6
  private Double pm10;            // value7
  private Double temp1;           // value8
  private Double temp2;           // value9
  private Double temp3;           // value10
  private Double noise;           // value11
  private Double nonContactTemp;  // value12
  private Double lux;             // value13

  public static SensorRealtimeDto entityToDto(Sensor s) {
    return SensorRealtimeDto.builder()
            .deviceCode(s.getDeviceCode())
            .timestamp(s.getMeasureDatetime() != null
                    ? s.getMeasureDatetime().toString()
                    : null)
            .temp(s.getValue1())
            .humidity(s.getValue2())
            .co2(s.getValue3())
            .voc(s.getValue4())
            .pm1(s.getValue5())
            .pm25(s.getValue6())
            .pm10(s.getValue7())
            .temp1(s.getValue8())
            .temp2(s.getValue9())
            .temp3(s.getValue10())
            .noise(s.getValue11())
            .nonContactTemp(s.getValue12())
            .lux(s.getValue13())
            .build();
  }
}
