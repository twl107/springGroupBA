package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.dbShop.DbProduct;
import com.example.springGroupBA.entity.dbShop.OrderItem;
import com.example.springGroupBA.entity.dbShop.ShopReview;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.dbShop.DbProductRepository;
import com.example.springGroupBA.repository.dbShop.OrderItemRepository;
import com.example.springGroupBA.repository.dbShop.ShopReviewRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ShopReviewService {

    private final ShopReviewRepository shopReviewRepository;
    private final MemberRepository memberRepository;
    private final DbProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadPath;

    public void registerReview(String email, Long productId, Long orderItemId, String content, int rating, MultipartFile reviewFile) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

        DbProduct product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품 정보가 없습니다."));

        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("주문 상품 정보가 없습니다."));

        if (shopReviewRepository.existsByOrderItem(orderItem)) {
            throw new IllegalStateException("이미 리뷰를 작성한 상품입니다.");
        }

        String reviewImageName = null;
        if (reviewFile != null && !reviewFile.isEmpty()) {
            reviewImageName = saveFile(reviewFile);
        }

        ShopReview review = ShopReview.builder()
                .member(member)
                .product(product)
                .orderItem(orderItem)
                .content(content)
                .rating(rating)
                .reviewImage(reviewImageName)
                .regDate(LocalDateTime.now())
                .build();

        shopReviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public ShopReview getReview(Long reviewId) {
        return shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));
    }

    public void modifyReview(String email, Long reviewId, String content, int rating, MultipartFile reviewFile) {
        ShopReview review = shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        if (!review.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 리뷰만 수정할 수 있습니다.");
        }

        String newImageName = review.getReviewImage();

        if (reviewFile != null && !reviewFile.isEmpty()) {
            if (newImageName != null) {
                deleteFile(newImageName);
            }
            newImageName = saveFile(reviewFile);
        }

        review.changeReview(content, rating, newImageName);
    }

    public void deleteReview(String email, Long reviewId) {
        ShopReview review = shopReviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰가 존재하지 않습니다."));

        if (!review.getMember().getEmail().equals(email)) {
            throw new IllegalArgumentException("본인의 리뷰만 삭제할 수 있습니다.");
        }

        if (review.getReviewImage() != null) {
            deleteFile(review.getReviewImage());
        }

        shopReviewRepository.delete(review);
    }

    private String saveFile(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String saveName = uuid + "_" + originalName;

        File folder = new File(uploadPath, "review");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        try {
            file.transferTo(new File(folder, saveName));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return saveName;
    }

    private void deleteFile(String fileName) {
        try {
            String decodedName = URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            File file = new File(uploadPath, "review/" + decodedName);
            if (file.exists()) file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}