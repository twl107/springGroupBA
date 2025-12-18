package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.entity.dbShop.QDbProduct;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;
import java.util.Objects;

public class DbProductSearchImpl extends QuerydslRepositorySupport implements DbProductSearch {

    public DbProductSearchImpl() {
        super(DbProduct.class);
    }

    @Override
    public Page<DbProduct> searchDbProduct(String statusMode, List<DbProductCategory> categories, String keyword, Pageable pageable) {

        QDbProduct product = QDbProduct.dbProduct;
        JPQLQuery<DbProduct> query = from(product);

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (statusMode != null && !statusMode.isEmpty()) {
            switch (statusMode) {
                case "SALE":
                    booleanBuilder.and(product.isDeleted.eq(false).and(product.stock.gt(0)));
                    break;
                case "SOLDOUT":
                    booleanBuilder.and(product.isDeleted.eq(false).and(product.stock.loe(0)));
                    break;
                case "STOP":
                    booleanBuilder.and(product.isDeleted.eq(true));
                    break;
            }
        }

        if (categories != null && !categories.isEmpty()) {
            booleanBuilder.and(product.category.in(categories));
        }

        if (keyword != null && keyword.trim().length() > 0) {
            BooleanBuilder conditionBuilder = new BooleanBuilder();
            conditionBuilder.or(product.name.contains(keyword));
            conditionBuilder.or(product.modelName.contains(keyword));
            booleanBuilder.and(conditionBuilder);
        }

        query.where(booleanBuilder);

        OrderSpecifier<Integer> statusOrder = new CaseBuilder()
                .when(product.isDeleted.isFalse().and(product.stock.gt(0)))
                .then(0)
                .otherwise(1)
                .asc();

        query.orderBy(statusOrder);

        Objects.requireNonNull(getQuerydsl()).applyPagination(pageable, query);

        List<DbProduct> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }

}