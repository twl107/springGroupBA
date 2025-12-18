package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.constant.sensor.SensorType;
import com.example.springGroupBA.dto.sensor.DailySensorDto;
import com.example.springGroupBA.entity.sensor.SensorRaw;
import com.example.springGroupBA.entity.sensor.SensorThreshold;
import com.example.springGroupBA.repository.sensor.SensorRawRepository;
import com.example.springGroupBA.repository.sensor.SensorThresholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyReportService {

  private final SensorRawRepository sensorRawRepo;
  private final SensorThresholdRepository thresholdRepo;

  public List<DailySensorDto> getDailyReport(
          LocalDate date,
          LocalTime time,
          String device
  ) {

    // 1️. 기준 시각
    LocalDateTime target = LocalDateTime.of(date, time);

    // 2️. target 기준 prev / next RAW
    SensorRaw prev = sensorRawRepo
            .findPrevRaw(device, target)
            .stream()
            .findFirst()
            .orElse(null);

    SensorRaw next = sensorRawRepo
            .findNextRaw(device, target)
            .stream()
            .findFirst()
            .orElse(null);

    List<DailySensorDto> result = new ArrayList<>();

    // 3️. 센서 1~20 자동 처리
    for (int i = 1; i <= 20; i++) {

      SensorType type = SensorType.fromFieldIndex(i);

      Double prevValue = extractValue(prev, i);
      Double nextValue = extractValue(next, i);

      Double value = null;

      // 4️⃣ 보간 로직
      if (prev != null && next != null) {
        value = interpolate(
                prevValue,
                nextValue,
                prev.getMeasureDatetime(),
                next.getMeasureDatetime(),
                target
        );
      } else if (prev != null) {
        value = prevValue;
      } else if (next != null) {
        value = nextValue;
      }

      // 5️⃣ Threshold 조회
      SensorThreshold threshold = thresholdRepo
              .findByDeviceCodeAndSensorType(device, type)
              .orElseThrow(() ->
                      new IllegalStateException(
                              "Threshold not found for " + device + "/" + type
                      )
              );

      String active = threshold.getActive();
      String location = threshold.getLocation();

      // 6️⃣ 값 없음
      if (value == null) {
        result.add(new DailySensorDto(
                null,
                "EMPTY",
                location,
                active
        ));
        continue;
      }

      // 7️⃣ GOOD / BAD 판정
      Double min = threshold.getMinValue();
      Double max = threshold.getMaxValue();

      boolean isBad =
              (min != null && value < min) ||
                      (max != null && value > max);

      String status = isBad ? "BAD" : "GOOD";

      result.add(new DailySensorDto(
              value,
              status,
              location,
              active
      ));
    }

    return result;
  }

  /* ======================================================
   * 보간(interpolation) 계산
   ====================================================== */
  private Double interpolate(
          Double prevValue,
          Double nextValue,
          LocalDateTime prevTime,
          LocalDateTime nextTime,
          LocalDateTime target
  ) {
    if (prevValue == null || nextValue == null) return null;
    if (prevTime.equals(nextTime)) return round(prevValue);

    long totalSec = Duration.between(prevTime, nextTime).getSeconds();
    long passedSec = Duration.between(prevTime, target).getSeconds();

    if (totalSec <= 0) return round(prevValue);

    double ratio = (double) passedSec / totalSec;

    double raw =
            prevValue + (nextValue - prevValue) * ratio;

    return round(raw);
  }

  private Double round(double value) {
    return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP) // 🔥 여기서 자릿수 제어
            .doubleValue();
  }

  /* ======================================================
   * SensorRaw → value 추출
   ====================================================== */
  private Double extractValue(SensorRaw s, int index) {
    if (s == null) return null;

    return switch (index) {
      case 1 -> s.getValue1();
      case 2 -> s.getValue2();
      case 3 -> s.getValue3();
      case 4 -> s.getValue4();
      case 5 -> s.getValue5();
      case 6 -> s.getValue6();
      case 7 -> s.getValue7();
      case 8 -> s.getValue8();
      case 9 -> s.getValue9();
      case 10 -> s.getValue10();
      case 11 -> s.getValue11();
      case 12 -> s.getValue12();
      case 13 -> s.getValue13();
      case 14 -> s.getValue14();
      case 15 -> s.getValue15();
      case 16 -> s.getValue16();
      case 17 -> s.getValue17();
      case 18 -> s.getValue18();
      case 19 -> s.getValue19();
      case 20 -> s.getValue20();
      default -> null;
    };
  }
}
