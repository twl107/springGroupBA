package com.example.springGroupBA.constant;

public enum InquiryStatus {
    WAITING("답변 대기"),
    COMPLETE("답변 완료");

    private final String description;

    InquiryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
