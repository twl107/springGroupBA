package com.example.springGroupBA.constant;

public enum ReportReason {
    SPAM("스팸/부적절한 홍보"),
    ABUSIVE("욕설/비하 발언"),
    ADULT("음란물/청소년 유해 정보"),
    POLITICAL("과도한 정치적 갈등 조장"),
    OTHER("기타 사유");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
