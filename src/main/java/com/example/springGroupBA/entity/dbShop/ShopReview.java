package com.example.springGroupBA.entity.dbShop;

import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ShopReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private DbProduct product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(nullable = false, length = 1000)
    private String content;

    private int rating;

    private String reviewImage;

    @CreatedDate
    private LocalDateTime regDate;

    public void changeReview(String content, int rating, String reviewImage) {
        this.content = content;
        this.rating = rating;
        this.reviewImage = reviewImage;
    }
}