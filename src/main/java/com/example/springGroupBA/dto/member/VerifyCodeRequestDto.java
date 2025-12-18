package com.example.springGroupBA.dto.member;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyCodeRequestDto {
  private String email;
  private String code;
}
