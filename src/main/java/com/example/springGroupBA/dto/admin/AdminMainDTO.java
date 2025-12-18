package com.example.springGroupBA.dto.admin;

import com.example.springGroupBA.entity.dbShop.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminMainDTO {
    private long newOrderCount;
    private long todaySales;
    private long monthSales;
    private long claimCount;

    private List<Order> recentOrders;
    private long preparingCount;
    private long communityBlindCount;
    private long newMemberCount;
    private long unansweredQnaCount;
}