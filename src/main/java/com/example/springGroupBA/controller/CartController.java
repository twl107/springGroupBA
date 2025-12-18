package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.dbShop.CartDetailDTO;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.service.CartService;
import com.example.springGroupBA.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;
  private final MemberRepository memberRepository;
  private final MemberService memberService;

  @GetMapping("/cartList")
  public String cartList(Principal principal, Model model) {
    if (principal == null) {
      return "redirect:/member/memberLogin";
    }

    List<CartDetailDTO> cartList = cartService.getCartList(principal.getName());
    model.addAttribute("cartList", cartList);
    return "dbShop/cartList";
  }

  @PostMapping("/addAsync")
  @ResponseBody
  public ResponseEntity<String> addCartAsync(@RequestParam("productId") Long productId,
                                             @RequestParam("count") int count,
                                             @AuthenticationPrincipal UserDetails user) {
    if (user == null) {
      return new ResponseEntity<>("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
    }

    try {
      Member member = memberRepository.findByEmail(user.getUsername())
              .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

      cartService.addCart(member.getId(), productId, count);

      return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
    }
    catch (Exception e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  @PostMapping("/update")
  public String updateCartItem(@RequestParam("cartItemId") Long cartItemId,
                               @RequestParam("count") int count) {
    if (count <= 0) {
      return "redirect:/cart/cartList";
    }
    cartService.updateCartItemCount(cartItemId, count);
    return "redirect:/cart/cartList";
  }

  @PostMapping("/delete")
  public String deleteCartItem(@RequestParam("cartItemId") Long cartItemId) {
    cartService.deleteCartItem(cartItemId);
    return "redirect:/cart/cartList";
  }

}
