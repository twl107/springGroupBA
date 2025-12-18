package com.example.springGroupBA.entity.dbShop;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"order", "product", "shopReview"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private DbProduct product;

    @OneToOne(mappedBy = "orderItem", fetch = FetchType.LAZY)
    private ShopReview shopReview;

    private int count;
    private int orderPrice;
}
