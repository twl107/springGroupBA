package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.dto.dbShop.CartDetailDTO;
import com.example.springGroupBA.entity.dbShop.CartItem;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem,Long> {

    CartItem findByCartIdAndProductId(Long cartId,Long productId);

    @Query("select new com.example.springGroupBA.dto.dbShop.CartDetailDTO(" +
            "ci.id, p.id, p.name, p.price, ci.count, p.mainImage, p.category) " +
            "from CartItem ci " +
            "join ci.product p " +
            "where ci.cart.id = :cartId " +
            "order by ci.id desc")
    List<CartDetailDTO> findCartDetailDtoList(@Param("cartId") Long cartId);

    void deleteByProduct(DbProduct product);
}
