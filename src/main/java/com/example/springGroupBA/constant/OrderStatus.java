package com.example.springGroupBA.constant;

public enum OrderStatus {
    PAYMENT_COMPLETED("결제완료"),
    PREPARING("배송준비중"),
    SHIPPING("배송중"),
    DELIVERED("배송완료"),
    CONFIRMED("구매확정"),
    CANCEL_REQUESTED("취소요청"),
    CANCEL_COMPLETED("취소완료"),
    RETURN_REQUESTED("반품요청"),
    RETURN_COMPLETED("반품완료");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
