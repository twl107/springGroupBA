package com.example.springGroupBA.dto.survey;

import com.example.springGroupBA.constant.SurveyQuestionType;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class QuestionStatsDto {
  private Long questionId;
  private String questionText;
  private SurveyQuestionType type; // single, multi, text, number

  private int totalResponses;

  // 단일/복수 선택용 통계
  private Map<String, Integer> countMap = new LinkedHashMap<>();
  private Map<String, Double> percentMap = new LinkedHashMap<>();

  // 숫자형 통계
  private Double avg;
  private Integer min;
  private Integer max;

  // 텍스트형 리스트
  private List<String> textResponses = new ArrayList<>();
}

