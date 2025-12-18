package com.example.springGroupBA.dto.member;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoDto {
  private Long id;
  private String email;
  private String nickName;

}
