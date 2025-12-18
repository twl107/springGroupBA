package com.example.springGroupBA.entity.survey;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "survey_meta")
@Getter
@Setter
public class SurveyMeta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // 설문 제목
  @Column(nullable = false)
  private String title;

  // 설명(선택)
  @Column(columnDefinition = "TEXT")
  private String description;

  // 설문 기간
  private LocalDateTime startDate;
  private LocalDateTime endDate;

  // 활성화 여부 (Y/N)
  @Column(nullable = false)
  private String active = "Y";

  // 생성/수정 시간
  private LocalDateTime createdAt = LocalDateTime.now();
  private LocalDateTime updatedAt = LocalDateTime.now();
}
