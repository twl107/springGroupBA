package com.example.springGroupBA.controller;

import com.example.springGroupBA.entity.CustomerReview;
import com.example.springGroupBA.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/review")
public class ReviewController {

  private final AdminService adminService;

  @GetMapping("/list")
  public String reviewList(@RequestParam(defaultValue = "0") int page,
                           Model model) {

    int size = 6;
    Pageable pageable = PageRequest.of(page, size);

    Page<CustomerReview> reviewPage = adminService.findVisibleReviews(pageable);

    model.addAttribute("reviewPage", reviewPage);
    model.addAttribute("currentPage", page);

    return "review/reviewList";
  }

  @GetMapping("/view/{id}")
  public String reviewView(@PathVariable Long id, Model model) {
    model.addAttribute("review", adminService.findReviewById(id));
    return "review/reviewView";
  }
}
