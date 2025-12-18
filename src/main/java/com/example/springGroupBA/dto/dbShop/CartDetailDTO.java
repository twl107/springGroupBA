package com.example.springGroupBA.dto.dbShop;

import com.example.springGroupBA.constant.DbProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartDetailDTO {

    private Long cartItemId;
    private Long productId;
    private String name;
    private int price;
    private int count;
    private String mainImage;
    private String category;

    public CartDetailDTO(Long cartItemId, Long productId, String name, int price,
                         int count, String mainImage, DbProductCategory categoryEnum) {
        this.cartItemId = cartItemId;
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.count = count;
        this.mainImage = mainImage;

        if (categoryEnum != null) {
            this.category = categoryEnum.toString();
        }
    }

    public String getCategoryDes() {
        if (this.category == null) return "";
        try {
            return DbProductCategory.valueOf(this.category).getDescription();
        }
        catch (Exception e) { return ""; }
    }

    public int getTotalPrice() {
        return price * count;
    }
}
