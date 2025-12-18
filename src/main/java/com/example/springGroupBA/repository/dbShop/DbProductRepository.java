package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.entity.dbShop.DbProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbProductRepository extends JpaRepository<DbProduct,Long>, DbProductSearch {

    @Query("SELECT p FROM DbProduct p " +
            "ORDER BY (CASE WHEN p.isDeleted = false AND p.stock > 0 THEN 0 ELSE 1 END) ASC, " +
            "p.id DESC")
    List<DbProduct> findAllSortedByStatus();

}
