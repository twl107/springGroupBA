package com.example.springGroupBA.entity.recruit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitConfig {

  @Id
  @Builder.Default
  private Long id = 1L;   // 단일 Row

  @Builder.Default
  private boolean open = true;  // true면 지원 가능

  // 직무별 설명
  @Column(columnDefinition = "TEXT")
  private String backendDesc;

  @Column(columnDefinition = "TEXT")
  private String frontDesc;

  @Column(columnDefinition = "TEXT")
  private String embeddedDesc;

  @Column(columnDefinition = "TEXT")
  private String aiDesc;

  @Column(columnDefinition = "TEXT")
  private String iotDesc;

  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
