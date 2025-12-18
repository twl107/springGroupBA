package com.example.springGroupBA.service;

import com.example.springGroupBA.dto.dbShop.CartDetailDTO;
import com.example.springGroupBA.entity.dbShop.Cart;
import com.example.springGroupBA.entity.dbShop.CartItem;
import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.dbShop.CartItemRepository;
import com.example.springGroupBA.repository.dbShop.CartRepository;
import com.example.springGroupBA.repository.dbShop.DbProductRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final DbProductRepository dbProductRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public Long addCart(Long memberId, Long productId, int count) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        DbProduct product = dbProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));

        Cart cart = cartRepository.findByMemberId(memberId);
        if (cart == null) {
            cart = Cart.createCart(member);
            cartRepository.save(cart);
        }

        CartItem savedCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId);

        if (savedCartItem != null) {
            savedCartItem.updateCount(savedCartItem.getCount() + count);
            return savedCartItem.getId();
        }
        else {
            CartItem cartItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .count(count)
                    .build();
            cartItemRepository.save(cartItem);
            return cartItem.getId();
        }
    }

    @Transactional(readOnly = true)
    public List<CartDetailDTO> getCartList(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        Cart cart = cartRepository.findByMemberId(member.getId());

        if (cart == null) {
            return new ArrayList<>();
        }
        return cartItemRepository.findCartDetailDtoList(cart.getId());
    }

    public void deleteCartItem(Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템이 없습니다."));
        cartItemRepository.delete(cartItem);
    }

    public void updateCartItemCount(Long cartItemId, int count) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("장바구니 아이템이 없습니다."));
        cartItem.updateCount(count);
    }

    public CartDetailDTO createDirectOrderItem(Long productId, int count) {
        DbProduct product = dbProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다."));

        return new CartDetailDTO(
                null,
                product.getId(),
                product.getName(),
                product.getPrice(),
                count,
                product.getMainImage(),
                product.getCategory()
        );
    }
}
