package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DbProductSearch {

    Page<DbProduct> searchDbProduct(String statusMode, List<DbProductCategory> categories, String keyword, Pageable pageable);
}
