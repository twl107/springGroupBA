package com.example.springGroupBA.constant;

public enum DbProductCategory {
    TEMP("온도 센서"),
    ENV("환경 센서"),
    SOUND("소음 센서"),
    LIGHT("조도 센서");

    private final String description;   // 필드
    DbProductCategory(String description) { // 생성자
        this.description = description;
    }
    public String getDescription() {    // 메서드
        return description;
    }
}
