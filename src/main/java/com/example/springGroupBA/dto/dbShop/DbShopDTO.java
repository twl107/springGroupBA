package com.example.springGroupBA.dto.dbShop;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DbShopDTO {

    private Long id;
    private String name;
    private String modelName;
    private String category;
    private int price;
    private int stock;
    private String description;

    private MultipartFile file;
    private String mainImage;
    private boolean isDeleted;
    private boolean imageDeleted;

    public String getCategoryDes() {
        if (this.category == null) {
            return "";
        }
        try {
            return DbProductCategory.valueOf(this.category).getDescription();
        }
        catch (IllegalArgumentException e) {
            return this.category;
        }
    }

    public static DbShopDTO entityToDto(DbProduct entity) {
        return DbShopDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .modelName(entity.getModelName())
                .category(entity.getCategory().name())
                .price(entity.getPrice())
                .stock(entity.getStock())
                .description(entity.getDescription())
                .mainImage(entity.getMainImage())
                .isDeleted(entity.isDeleted())
                .build();
    }
}
