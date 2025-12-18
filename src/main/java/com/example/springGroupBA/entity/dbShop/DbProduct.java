package com.example.springGroupBA.entity.dbShop;

import com.example.springGroupBA.constant.DbProductCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String modelName;

    @Enumerated(EnumType.STRING)
    private DbProductCategory category;

    private int price;
    private int stock;

    @Column(length = 2000)
    private String description;

    private String mainImage;

    @Builder.Default
    private boolean isDeleted = false;

    public void markAsDeleted() {
        this.isDeleted = true;
    }

    public void markAsActive() {
        this.isDeleted = false;
    }

}
