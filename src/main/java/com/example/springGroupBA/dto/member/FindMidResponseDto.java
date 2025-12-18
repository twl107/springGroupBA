package com.example.springGroupBA.dto.member;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindMidResponseDto {
  public List<String> memberIds;
  private String message;
}
