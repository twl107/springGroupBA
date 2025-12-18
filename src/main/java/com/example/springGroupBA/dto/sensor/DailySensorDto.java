package com.example.springGroupBA.dto.sensor;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailySensorDto {
  private Double value;    // 센서 값
  private String status;   // GOOD / BAD / EMPTY
  private String location; // 1F / 자재실 / 냉장고 등
  private String active;  // ★ 추가
}
