package com.example.springGroupBA.entity.recruit;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Applicant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String email;

  private String phone;

  @Column(columnDefinition = "TEXT")
  private String intro;

  private String resumePath;

  private LocalDateTime createdAt;

  // =============================
  // 🔥 추가된 기능
  // =============================

  @Builder.Default
  private String position = "미지정";

  // 지원 상태 (검토중 / 합격 / 불합격)
  // DB에 null 들어감 방지 → default 설정
  @Column(nullable = false)
  @Builder.Default
  private String status = "검토중";

  // 관리자 전용 메모 (지원자에게 공개되지 않음)
  @Column(columnDefinition = "TEXT")
  private String adminMemo;


  @PrePersist
  public void prePersist() {
    this.createdAt = LocalDateTime.now();
  }
}
