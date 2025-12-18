package com.example.springGroupBA.dto.dbShop;

import lombok.Data;

import java.util.List;

@Data
public class PaymentDTO {
    private String impUid;
    private String merchantUid;
    private int paidAmount;

    private Long productId;
    private int count;

    private List<Long> cartItemIds;

    private String receiverName;
    private String receiverPhone;
    private String address;
    private String deliveryRequest;

}
