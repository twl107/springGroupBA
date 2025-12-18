package com.example.springGroupBA.entity.survey;

import com.example.springGroupBA.constant.SurveyQuestionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "survey_question")
@Getter
@Setter
public class SurveyQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 어떤 설문(meta)에 속하는지
  @Column(nullable = false)
  private Long surveyId;

  // 질문 내용
  @Column(nullable = false, columnDefinition = "TEXT")
  private String questionText;

  // 질문 타입 (SINGLE, MULTI, TEXT 등)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SurveyQuestionType questionType = SurveyQuestionType.SINGLE;

  // 문항 순서 (optional, default=0)
  @Column(nullable = false)
  private Integer sortOrder = 0;

  // 필수 여부
  @Column(nullable = false)
  private String required = "Y";
}
