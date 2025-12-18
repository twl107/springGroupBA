package com.example.springGroupBA.entity.survey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "survey_result")
public class SurveyResult {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 설문 제출한 회원 PK
  private Long memberId;

  @Column(nullable = false)
  private Long surveyId;

  @Column(columnDefinition = "TEXT")
  private String answersJson;   // <-- 이거 추가

  private LocalDateTime createdAt = LocalDateTime.now();
}
