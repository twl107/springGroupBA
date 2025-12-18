package com.example.springGroupBA.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReview {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String writerName;

  private String writerEmail;

  private String title;

  @Column(columnDefinition = "TEXT")
  private String content;

  private Integer rating;  // 1~5 점수

  private String imagePath;

  @Builder.Default
  private String visible = "Y";  // Y: 공개, N: 비공개

  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();

  @Builder.Default
  private LocalDateTime updatedAt = LocalDateTime.now();
}
