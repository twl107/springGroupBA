package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.entity.dbShop.Order;
import com.example.springGroupBA.entity.dbShop.QOrder;
import com.example.springGroupBA.entity.member.QMember;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrderSearchImpl extends QuerydslRepositorySupport implements OrderSearch {

    public OrderSearchImpl() {
        super(Order.class);
    }

    @Override
    public Page<Order> searchOrder(Long memberId, String type, String keyword, String startDate, String endDate, OrderStatus status, Pageable pageable) {

        QOrder order = new QOrder("orderInfo");
        QMember member = QMember.member;

        JPQLQuery<Order> query = from(order);
        query.leftJoin(order.member, member);

        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (memberId != null) {
            booleanBuilder.and(member.id.eq(memberId));
        }

        if (status != null) {
            booleanBuilder.and(order.status.eq(status));
        }

        if (type != null && keyword != null && !keyword.trim().isEmpty()) {
            BooleanBuilder searchBuilder = new BooleanBuilder();

            switch (type) {
                case "n":
                    searchBuilder.or(member.name.contains(keyword));
                    break;
                case "r":
                    searchBuilder.or(order.receiverName.contains(keyword));
                    break;
                case "p":
                    searchBuilder.or(order.orderItems.any().product.name.contains(keyword));
                    break;
                case "nr":
                    searchBuilder.or(member.name.contains(keyword));
                    searchBuilder.or(order.receiverName.contains(keyword));
                    break;
            }
            booleanBuilder.and(searchBuilder);
        }

        if (startDate != null && !startDate.isEmpty()) {
            LocalDateTime start = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE).atStartOfDay();
            booleanBuilder.and(order.orderDate.goe(start));
        }
        if (endDate != null && !endDate.isEmpty()) {
            LocalDateTime end = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE).atTime(LocalTime.MAX);
            booleanBuilder.and(order.orderDate.loe(end));
        }

        query.where(booleanBuilder);

        PathBuilder<Order> entityPath = new PathBuilder<>(Order.class, "orderInfo");
        Querydsl querydsl = new Querydsl(getEntityManager(), entityPath);
        querydsl.applyPagination(pageable, query);

        List<Order> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }
}