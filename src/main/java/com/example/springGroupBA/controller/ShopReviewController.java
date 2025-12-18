package com.example.springGroupBA.controller;

import com.example.springGroupBA.entity.dbShop.ShopReview;
import com.example.springGroupBA.service.ShopReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/shopReview")
@RequiredArgsConstructor
public class ShopReviewController {

    private final ShopReviewService shopReviewService;

    @PostMapping("/registerAsync")
    @ResponseBody
    public ResponseEntity<String> registerReviewAsync(@AuthenticationPrincipal UserDetails user,
                                                      @RequestParam Long productId,
                                                      @RequestParam Long orderItemId,
                                                      @RequestParam String content,
                                                      @RequestParam int rating,
                                                      @RequestParam(required = false) MultipartFile reviewFile) {
        if (user == null) return new ResponseEntity<>("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);

        try {
            shopReviewService.registerReview(user.getUsername(), productId, orderItemId, content, rating, reviewFile);
            return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/{reviewId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReview(@PathVariable Long reviewId) {
        try {
            ShopReview review = shopReviewService.getReview(reviewId);

            Map<String, Object> response = new HashMap<>();
            response.put("id", review.getId());
            response.put("content", review.getContent());
            response.put("rating", review.getRating());
            response.put("reviewImage", review.getReviewImage());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/modify")
    @ResponseBody
    public ResponseEntity<String> modifyReview(@AuthenticationPrincipal UserDetails user,
                                               @RequestParam Long reviewId,
                                               @RequestParam String content,
                                               @RequestParam int rating,
                                               @RequestParam(required = false) MultipartFile reviewFile) {
        if (user == null) return new ResponseEntity<>("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);

        try {
            shopReviewService.modifyReview(user.getUsername(), reviewId, content, rating, reviewFile);
            return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/delete/{reviewId}")
    @ResponseBody
    public ResponseEntity<String> deleteReview(@AuthenticationPrincipal UserDetails user,
                                               @PathVariable Long reviewId) {
        if (user == null) return new ResponseEntity<>("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);

        try {
            shopReviewService.deleteReview(user.getUsername(), reviewId);
            return new ResponseEntity<>("SUCCESS", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}