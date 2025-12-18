package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.entity.dbShop.OrderItem;
import com.example.springGroupBA.entity.dbShop.ShopReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    List<ShopReview> findByProductIdOrderByRegDateDesc(Long productId);

    boolean existsByOrderItem(OrderItem orderItem);

    Optional<ShopReview> findByOrderItemId(Long orderItemId);
}