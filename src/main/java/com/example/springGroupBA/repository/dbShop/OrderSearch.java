package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.entity.dbShop.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderSearch {

    Page<Order> searchOrder(Long memberId, String type, String keyword, String startDate, String endDate, OrderStatus status, Pageable pageable);
}
