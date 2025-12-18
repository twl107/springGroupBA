package com.example.springGroupBA.constant.sensor;

import lombok.Getter;

@Getter
public enum SensorType {

  // 1~13 (기존 센서)
  TEMP("실내온도"),
  HUMIDITY("상대습도"),
  CO2("이산화탄소"),
  VOC("유기화합물 VOC"),

  PM1("초미세먼지 PM1.0"),
  PM25("초미세먼지 PM2.5"),
  PM10("미세먼지 PM10"),

  T1("온도_1"),
  T2("온도_2"),
  T3("온도_3"),

  NOISE("소음"),
  TNC("TNC"),
  LUX("조도"),

  // 14~20 (확장 센서)
  VAL14("센서14"),
  VAL15("센서15"),
  VAL16("센서16"),
  VAL17("센서17"),
  VAL18("센서18"),
  VAL19("센서19"),
  VAL20("센서20");

  private final String label;

  SensorType(String label) {
    this.label = label;
  }

  /**
   * SensorRaw.value1~value20 → 센서타입의 값을 가져오기
   */
  public Double getValue(com.example.springGroupBA.entity.sensor.SensorRaw s) {
    return switch (this) {
      case TEMP -> s.getValue1();
      case HUMIDITY -> s.getValue2();
      case CO2 -> s.getValue3();
      case VOC -> s.getValue4();

      case PM1 -> s.getValue5();
      case PM25 -> s.getValue6();
      case PM10 -> s.getValue7();

      case T1 -> s.getValue8();
      case T2 -> s.getValue9();
      case T3 -> s.getValue10();

      case NOISE -> s.getValue11();
      case TNC -> s.getValue12();
      case LUX -> s.getValue13();

      case VAL14 -> s.getValue14();
      case VAL15 -> s.getValue15();
      case VAL16 -> s.getValue16();
      case VAL17 -> s.getValue17();
      case VAL18 -> s.getValue18();
      case VAL19 -> s.getValue19();
      case VAL20 -> s.getValue20();
    };
  }

  /**
   * 한글/영문/라벨/약어 변환
   */
  public static SensorType fromAny(String value) {
    if (value == null) return null;
    String v = value.trim().toUpperCase();

    // 1) ENUM 이름 완전 일치
    for (SensorType t : SensorType.values()) {
      if (t.name().equalsIgnoreCase(v)) return t;
    }

    // 2) 라벨 매칭
    for (SensorType t : SensorType.values()) {
      if (t.getLabel() != null && t.getLabel().equalsIgnoreCase(value))
        return t;
    }

    // 3) 약어 / 변형 입력 처리
    return switch (v) {
      case "PM1.0" -> PM1;
      case "PM2.5" -> PM25;

      case "TEMP1", "T1" -> T1;
      case "TEMP2", "T2" -> T2;
      case "TEMP3", "T3" -> T3;

      case "SENSOR14", "VAL14" -> VAL14;
      case "SENSOR15", "VAL15" -> VAL15;
      case "SENSOR16", "VAL16" -> VAL16;
      case "SENSOR17", "VAL17" -> VAL17;
      case "SENSOR18", "VAL18" -> VAL18;
      case "SENSOR19", "VAL19" -> VAL19;
      case "SENSOR20", "VAL20" -> VAL20;

      default -> null;
    };
  }

  /**
   * value_1~value_20 → SensorType 매핑
   */
  public static SensorType fromFieldIndex(int index) {
    return switch (index) {
      case 1 -> TEMP;
      case 2 -> HUMIDITY;
      case 3 -> CO2;
      case 4 -> VOC;

      case 5 -> PM1;
      case 6 -> PM25;
      case 7 -> PM10;

      case 8 -> T1;
      case 9 -> T2;
      case 10 -> T3;

      case 11 -> NOISE;
      case 12 -> TNC;
      case 13 -> LUX;

      case 14 -> VAL14;
      case 15 -> VAL15;
      case 16 -> VAL16;
      case 17 -> VAL17;
      case 18 -> VAL18;
      case 19 -> VAL19;
      case 20 -> VAL20;

      default -> null;
    };
  }
}
