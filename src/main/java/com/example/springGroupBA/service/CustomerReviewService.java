package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.CustomerReview;
import com.example.springGroupBA.repository.CustomerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerReviewService {

  private final CustomerReviewRepository reviewRepository;

  public List<CustomerReview> findAll() {
    return reviewRepository.findAll();
  }

  public CustomerReview findById(Long id) {
    return reviewRepository.findById(id).orElse(null);
  }

  public CustomerReview save(CustomerReview review) {
    return reviewRepository.save(review);
  }

  public void delete(Long id) {
    reviewRepository.deleteById(id);
  }

  public void toggleVisible(Long id) {
    CustomerReview review = findById(id);
    if (review != null) {
      review.setVisible(review.getVisible().equals("Y") ? "N" : "Y");
      reviewRepository.save(review);
    }
  }
}
