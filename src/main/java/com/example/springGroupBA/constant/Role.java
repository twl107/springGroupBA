package com.example.springGroupBA.constant;

public enum Role {
  ADMIN("관리자"),
  VIP("우수회원"),
  USER("정회원"),
  JUNIOR("준회원");

  private final String label;

  Role(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}
