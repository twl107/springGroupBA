package com.example.springGroupBA.repository;

import com.example.springGroupBA.entity.CustomerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerReviewRepository extends JpaRepository<CustomerReview, Long> {

  List<CustomerReview> findByVisible(String y);

  // visible = 'Y' 인 후기만, 최신순
  Page<CustomerReview> findByVisibleOrderByCreatedAtDesc(String visible, Pageable pageable);
}
