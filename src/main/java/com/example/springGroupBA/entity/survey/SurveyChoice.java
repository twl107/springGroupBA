package com.example.springGroupBA.entity.survey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "survey_choice")
@Getter
@Setter
public class SurveyChoice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 어느 질문에 속하는가?
  @Column(nullable = false)
  private Long questionId;

  // 보기 내용
  @Column(nullable = false, columnDefinition = "TEXT")
  private String choiceText;

  // 선택지 순서 (0,1,2,...)
  @Column(nullable = false)
  private Integer sortOrder = 0;
}
