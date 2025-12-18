package com.example.springGroupBA.controller;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.dbShop.DbShopDTO;
import com.example.springGroupBA.entity.dbShop.ShopReview;
import com.example.springGroupBA.repository.dbShop.ShopReviewRepository;
import com.example.springGroupBA.service.DbShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/dbShop")
@RequiredArgsConstructor
public class DbShopController {

  private final DbShopService dbShopService;
  private final ShopReviewRepository shopReviewRepository;

  @GetMapping("/dbShopList")
  public String dbShopListGet(PageRequestDTO pageRequestDTO,
                              @RequestParam(value = "category", required = false) List<DbProductCategory> categories,
                              Model model) {

    model.addAttribute("result", dbShopService.getDbShopList(pageRequestDTO, null, categories));
    model.addAttribute("categories", categories);
    return "dbShop/dbShopList";
  }

  @GetMapping("/dbProductContent")
  public String dbShopContentGet(@RequestParam("id") Long id, Model model) {
    DbShopDTO dto = dbShopService.getDbProductContent(id);
    model.addAttribute("dto", dto);

    List<ShopReview> reviews = shopReviewRepository.findByProductIdOrderByRegDateDesc(id);
    model.addAttribute("reviews", reviews);

    return "dbShop/dbProductContent";
  }

}
