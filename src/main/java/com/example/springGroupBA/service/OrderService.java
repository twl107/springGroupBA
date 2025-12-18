package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.dbShop.CartDetailDTO;
import com.example.springGroupBA.dto.dbShop.PaymentDTO;
import com.example.springGroupBA.entity.dbShop.CartItem;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.entity.dbShop.Order;
import com.example.springGroupBA.entity.dbShop.OrderItem;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.dbShop.CartItemRepository;
import com.example.springGroupBA.repository.dbShop.DbProductRepository;
import com.example.springGroupBA.repository.dbShop.OrderRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final MemberRepository memberRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final DbProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CartDetailDTO> getSelectedCartItems(List<Long> cartItemIds) {
        List<CartItem> cartItems = cartItemRepository.findAllById(cartItemIds);
        return cartItems.stream().map(ci -> CartDetailDTO.builder()
                .cartItemId(ci.getId())
                .productId(ci.getProduct().getId())
                .name(ci.getProduct().getName())
                .price(ci.getProduct().getPrice())
                .count(ci.getCount())
                .mainImage(ci.getProduct().getMainImage())
                .category(ci.getProduct().getCategory().name())
                .build()).collect(Collectors.toList());
    }

    public void processOrder(String email, PaymentDTO paymentDTO) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        List<CartItem> orderTargets = new ArrayList<>();

        if (paymentDTO.getProductId() != null && paymentDTO.getProductId() > 0) {
            DbProduct product = productRepository.findById(paymentDTO.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("상품 정보가 없습니다."));

            CartItem directItem = CartItem.builder()
                    .product(product)
                    .count(paymentDTO.getCount())
                    .build();
            orderTargets.add(directItem);
        }
        else {
            if (paymentDTO.getCartItemIds() == null || paymentDTO.getCartItemIds().isEmpty()) {
                throw new IllegalArgumentException("주문할 상품이 없습니다.");
            }
            orderTargets = cartItemRepository.findAllById(paymentDTO.getCartItemIds());
        }

        if (orderTargets.isEmpty()) throw new IllegalArgumentException("주문 대상이 유효하지 않습니다.");

        int expectedTotalPrice = 0;
        for (CartItem ci : orderTargets) {
            expectedTotalPrice += ci.getProduct().getPrice() * ci.getCount();
        }

        Order order = Order.builder()
                .member(member)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PAYMENT_COMPLETED)
                .totalPrice(expectedTotalPrice)
                .receiverName(paymentDTO.getReceiverName())
                .receiverPhone(paymentDTO.getReceiverPhone())
                .address(paymentDTO.getAddress())
                .deliveryRequest(paymentDTO.getDeliveryRequest())
                .impUid(paymentDTO.getImpUid())
                .merchantUid(paymentDTO.getMerchantUid())
                .build();

        for (CartItem ci : orderTargets) {
            DbProduct product = ci.getProduct();
            if (product.getStock() < ci.getCount()) {
                throw new IllegalArgumentException("재고 부족: " + product.getName());
            }
            product.setStock(product.getStock() - ci.getCount());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .count(ci.getCount())
                    .orderPrice(ci.getProduct().getPrice())
                    .build();

            order.getOrderItems().add(orderItem);
        }

        orderRepository.save(order);

        if (paymentDTO.getCartItemIds() != null && !paymentDTO.getCartItemIds().isEmpty()) {
            List<CartItem> itemsToDelete = orderTargets.stream()
                    .filter(item -> item.getId() != null)
                    .collect(Collectors.toList());
            if(!itemsToDelete.isEmpty()) {
                cartItemRepository.deleteAll(itemsToDelete);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrderList(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return orderRepository.findByMemberId(member.getId());
    }

    public void confirmOrder(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        Member currentMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(currentMember.getId())) {
            throw new IllegalArgumentException("본인의 주문만 구매확정 할 수 있습니다.");
        }

        if (order.getStatus() == OrderStatus.DELIVERED) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.saveAndFlush(order);
        } else {
            throw new IllegalStateException("배송완료 상태에서만 가능합니다.");
        }
    }

    public void requestCancel(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        Member currentMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(currentMember.getId())) {
            throw new IllegalArgumentException("본인의 주문만 취소할 수 있습니다.");
        }

        List<OrderStatus> cancellable = Arrays.asList(
                OrderStatus.PAYMENT_COMPLETED, OrderStatus.PREPARING,
                OrderStatus.SHIPPING, OrderStatus.DELIVERED
        );

        if (cancellable.contains(order.getStatus())) {
            order.setStatus(OrderStatus.CANCEL_REQUESTED);
            orderRepository.saveAndFlush(order);
        } else {
            throw new IllegalStateException("취소 요청 불가 상태");
        }
    }

    public void requestReturn(Long orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문 정보를 찾을 수 없습니다."));

        Member currentMember = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        if (!order.getMember().getId().equals(currentMember.getId())) {
            throw new IllegalArgumentException("본인의 주문만 반품할 수 있습니다.");
        }

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            order.setStatus(OrderStatus.RETURN_REQUESTED);
            orderRepository.saveAndFlush(order);
        } else {
            throw new IllegalStateException("구매확정 상태에서만 가능");
        }
    }

    @Transactional
    public void updateOrdersStatus(List<Long> orderIds, OrderStatus status) {
        if (orderIds == null || orderIds.isEmpty()) return;
        List<Order> orders = orderRepository.findAllById(orderIds);
        for (Order order : orders) {
            order.setStatus(status);
        }
    }

    public void updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
    }

    @Transactional(readOnly = true)
    public PageResultDTO<Order, Order> getAdminOrderList(PageRequestDTO requestDTO, String startDate, String endDate, OrderStatus status) {
        Pageable pageable = requestDTO.getPageable(Sort.by("orderDate").descending());
        Page<Order> result = orderRepository.searchOrder(
                null,
                requestDTO.getType(),
                requestDTO.getKeyword(),
                startDate,
                endDate,
                status,
                pageable
        );
        Function<Order, Order> fn = (entity -> entity);
        return new PageResultDTO<>(result, fn);
    }

    @Transactional(readOnly = true)
    public PageResultDTO<Order, Order> getUserOrderList(String email, PageRequestDTO requestDTO, String startDate, String endDate, OrderStatus status) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        Long memberId = member.getId();

        Pageable pageable = requestDTO.getPageable(Sort.by("orderDate").descending());

        Page<Order> result = orderRepository.searchOrder(
                memberId,
                "p",
                requestDTO.getKeyword(),
                startDate,
                endDate,
                status,
                pageable
        );

        result.getContent().forEach(order -> {
            Hibernate.initialize(order.getOrderItems());
            order.getOrderItems().forEach(item -> {
                Hibernate.initialize(item.getProduct());
            });
        });

        Function<Order, Order> fn = (entity -> entity);
        return new PageResultDTO<>(result, fn);
    }

}