package com.example.springGroupBA.dto.member;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindMidRequestDto {
  public String email;
  public String tel;
}
