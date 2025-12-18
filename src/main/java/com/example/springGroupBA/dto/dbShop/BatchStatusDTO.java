package com.example.springGroupBA.dto.dbShop;

import com.example.springGroupBA.constant.OrderStatus;
import lombok.Data;

import java.util.List;

@Data
public class BatchStatusDTO {
    private List<Long> orderIds;
    private OrderStatus status;
}
