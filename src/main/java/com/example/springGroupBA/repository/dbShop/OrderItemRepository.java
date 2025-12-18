package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.entity.dbShop.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

    boolean existsByProduct(DbProduct product);
}
