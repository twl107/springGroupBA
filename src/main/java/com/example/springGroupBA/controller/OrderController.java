package com.example.springGroupBA.controller;

import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.dbShop.CartDetailDTO;
import com.example.springGroupBA.dto.dbShop.PaymentDTO;
import com.example.springGroupBA.entity.dbShop.Order;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.service.CartService;
import com.example.springGroupBA.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;
  private final CartService cartService;
  private final MemberRepository memberRepository;

  @Value("${portOne.code}")
  private String portOneCode;

  @PostMapping("/checkout")
  public String checkoutPage(@RequestParam("cartItemIds") List<Long> cartItemIds,
                             @AuthenticationPrincipal UserDetails user,
                             Model model) {
    if (user == null) return "redirect:/member/memberLogin";

    List<CartDetailDTO> orderList = orderService.getSelectedCartItems(cartItemIds);
    if (orderList.isEmpty()) return "redirect:/dbShop/cartList";

    Member member = memberRepository.findByEmail(user.getUsername()).orElse(new Member());
    int totalAmount = orderList.stream().mapToInt(CartDetailDTO::getTotalPrice).sum();

    model.addAttribute("orderList", orderList);
    model.addAttribute("member", member);
    model.addAttribute("totalAmount", totalAmount);
    model.addAttribute("portOneCode", portOneCode);

    return "dbShop/orderCheckout";
  }

  @PostMapping("/direct")
  public String orderDirect(@RequestParam("productId") Long productId,
                            @RequestParam("count") int count,
                            Principal principal,
                            Model model) {

    if (principal == null) return "redirect:/member/memberLogin";

    CartDetailDTO item = cartService.createDirectOrderItem(productId, count);
    List<CartDetailDTO> orderList = List.of(item);
    int totalAmount = item.getTotalPrice();
    Member member = memberRepository.findByEmail(principal.getName()).orElse(new Member());

    model.addAttribute("orderList", orderList);
    model.addAttribute("totalAmount", totalAmount);
    model.addAttribute("portOneCode", portOneCode);
    model.addAttribute("member", member);
    model.addAttribute("isDirect", true);

    return "dbShop/orderCheckout";
  }

  @PostMapping("/payment/complete")
  @ResponseBody
  public ResponseEntity<String> paymentComplete(@RequestBody PaymentDTO paymentDTO,
                                                @AuthenticationPrincipal UserDetails user) {
    if (user == null) return new ResponseEntity<>("로그인 필요", HttpStatus.UNAUTHORIZED);

    try {
      orderService.processOrder(user.getUsername(), paymentDTO);
      return new ResponseEntity<>("주문 완료", HttpStatus.OK);
    } catch (Exception e) {
      log.error("주문 에러", e);
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }

  @GetMapping("/history")
  public String orderHistory(Principal principal,
                             PageRequestDTO pageRequestDTO,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestParam(required = false) OrderStatus status,
                             Model model) {

    String email = principal.getName();

    PageResultDTO<Order, Order> result = orderService.getUserOrderList(email, pageRequestDTO, startDate, endDate, status);

    model.addAttribute("result", result);
    model.addAttribute("orderStatusList", OrderStatus.values());
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    model.addAttribute("searchStartDate", startDate);
    model.addAttribute("searchStatus", status);

    return "dbShop/orderHistory";
  }

  @PostMapping("/confirm")
  public String confirmOrder(@RequestParam Long orderId,
                             @AuthenticationPrincipal UserDetails user,
                             RedirectAttributes rttr) {
    if (user == null) return "redirect:/member/memberLogin";

    try {
      orderService.confirmOrder(orderId, user.getUsername());
      rttr.addFlashAttribute("msg", "구매 확정 처리되었습니다.");
    } catch (Exception e) {
      rttr.addFlashAttribute("msg", e.getMessage());
    }
    return "redirect:/order/history";
  }

  @PostMapping("/cancelRequest")
  public String cancelRequest(@RequestParam Long orderId,
                              @AuthenticationPrincipal UserDetails user,
                              RedirectAttributes rttr) {
    if (user == null) return "redirect:/member/memberLogin";

    try {
      orderService.requestCancel(orderId, user.getUsername());
      rttr.addFlashAttribute("msg", "취소 요청이 접수되었습니다.");
    } catch (Exception e) {
      rttr.addFlashAttribute("msg", e.getMessage());
    }
    return "redirect:/order/history";
  }

  @PostMapping("/returnRequest")
  public String returnRequest(@RequestParam Long orderId,
                              @AuthenticationPrincipal UserDetails user,
                              RedirectAttributes rttr) {
    if (user == null) return "redirect:/member/memberLogin";

    try {
      orderService.requestReturn(orderId, user.getUsername());
      rttr.addFlashAttribute("msg", "반품 요청이 접수되었습니다.");
    } catch (Exception e) {
      rttr.addFlashAttribute("msg", e.getMessage());
    }
    return "redirect:/order/history";
  }

}