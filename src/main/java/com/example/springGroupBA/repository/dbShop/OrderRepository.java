package com.example.springGroupBA.repository.dbShop;

import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.entity.dbShop.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, OrderSearch {

    @Query("select o from Order o where o.member.id = :memberId order by o.id desc")
    List<Order> findByMemberId(@Param("memberId") Long memberId);

    List<Order> findTop5ByOrderByOrderDateDesc();

    long countByStatus(OrderStatus status);

    @Query("select count(o) from Order o where o.status in :statuses")
    long countByStatusList(@Param("statuses") List<OrderStatus> statuses);

    @Query("select sum(o.totalPrice) from Order o where o.orderDate between :start and :end and o.status != 'CANCELLED'")
    Long sumTotalPriceBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}