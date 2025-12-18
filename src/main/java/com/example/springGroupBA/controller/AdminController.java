package com.example.springGroupBA.controller;

import com.example.springGroupBA.constant.DbProductCategory;
import com.example.springGroupBA.constant.OrderStatus;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.admin.AdminMainDTO;
import com.example.springGroupBA.dto.admin.ReportHistoryDTO;
import com.example.springGroupBA.dto.dbShop.BatchStatusDTO;
import com.example.springGroupBA.dto.dbShop.DbShopDTO;
import com.example.springGroupBA.dto.survey.QuestionStatsDto;
import com.example.springGroupBA.entity.recruit.Applicant;
import com.example.springGroupBA.entity.CustomerReview;
import com.example.springGroupBA.entity.recruit.RecruitConfig;
import com.example.springGroupBA.entity.survey.SurveyChoice;
import com.example.springGroupBA.entity.survey.SurveyMeta;
import com.example.springGroupBA.entity.survey.SurveyQuestion;
import com.example.springGroupBA.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

  private final AdminService adminService;
  private final ApplicantService applicantService;
  private final SurveyService surveyService;
  private final RecruitService recruitService;
  private final OrderService orderService;

  @Value("${org.zerock.upload.path}")
  private String uploadPath;

    /* ===========================
       고객후기 관리
    ============================ */

  // 리스트
  @GetMapping("/review/list")
  public String reviewList(@RequestParam(defaultValue = "0") int page,
                           Model model) {

    Page<CustomerReview> reviewPage = adminService.getReviewPage(page);

    model.addAttribute("reviewPage", reviewPage);
    model.addAttribute("currentPage", page);

    return "admin/review/adminReviewList";
  }

  // 상세보기
  @GetMapping("/review/view/{id}")
  public String reviewView(@PathVariable Long id, Model model) {
    model.addAttribute("review", adminService.findReviewById(id));
    return "admin/review/adminReviewView";
  }

  // 작성 폼
  @GetMapping("/review/write")
  public String reviewWriteForm(Model model) {
    model.addAttribute("review", new CustomerReview());
    return "admin/review/adminReviewWrite";
  }

  // 작성 처리
  @PostMapping("/review/write")
  public String reviewWrite(CustomerReview review,
                            @RequestParam("uploadFile") MultipartFile file) {

    adminService.saveReview(review, file);
    return "redirect:/admin/review/list";
  }

  // 수정 처리
  @PostMapping("/review/edit")
  public String reviewEdit(CustomerReview review,
                           @RequestParam(name = "uploadFile", required = false) MultipartFile file) {

    adminService.saveReview(review, file);
    return "redirect:/admin/review/list";
  }

  @GetMapping("/review/edit/{id}")
  public String reviewEditForm(@PathVariable Long id, Model model) {

    CustomerReview review = adminService.findReviewById(id);
    model.addAttribute("review", review);

    return "admin/review/adminReviewEdit";
  }

  // 삭제
  @GetMapping("/review/delete/{id}")
  public String reviewDelete(@PathVariable Long id) {
    adminService.deleteReview(id);
    return "redirect:/admin/review/list";
  }

  // 노출 토글
  @GetMapping("/review/visible/{id}")
  public String reviewVisible(@PathVariable Long id) {
    adminService.toggleReviewVisible(id);
    return "redirect:/admin/review/list";
  }


    /* ===========================
       지원서 관리
    ============================ */

  //지원자 목록
  @GetMapping("/recruit/applicants")
  public String applicantList(
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(required = false) String status,
          @RequestParam(required = false) String position,
          Model model) {

    PageRequest pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());

    Page<Applicant> applicantPage;

    // status, position 모두 조건 없는 경우
    if ((status == null || status.equals("전체")) &&
            (position == null || position.equals("전체"))) {

      applicantPage = applicantService.findAll(pageable);
    }
    // status만 선택됨
    else if (position == null || position.equals("전체")) {
      applicantPage = applicantService.findByStatus(status, pageable);
    }
    // position만 선택됨
    else if (status == null || status.equals("전체")) {
      applicantPage = applicantService.findByPosition(position, pageable);
    }
    // 둘 다 선택됨
    else {
      applicantPage = applicantService.findByStatusAndPosition(status, position, pageable);
    }

    // Model 세팅
    model.addAttribute("applicantPage", applicantPage);
    model.addAttribute("currentPage", page);
    model.addAttribute("status", status);
    model.addAttribute("position", position);

    return "admin/recruit/applicantList";
  }


  // 단일 삭제
  @GetMapping("/recruit/applicants/delete/{id}")
  public String delete(@PathVariable Long id) {
    applicantService.delete(id);
    return "redirect:/admin/recruit/applicants";
  }

  // 선택 삭제
  @PostMapping("/recruit/applicants/delete")
  public String deleteSelected(
          @RequestParam(value = "ids", required = false) List<Long> ids) {

    if (ids == null || ids.isEmpty()) {
      // 아무것도 선택 안했을 때
      return "redirect:/admin/recruit/applicants?error=none";
    }

    applicantService.deleteAll(ids);
    return "redirect:/admin/recruit/applicants?deleted=" + ids.size();
  }

  //지원자 상세보기
  @GetMapping("/recruit/applicants/{id}")
  public String applicantView(@PathVariable Long id, Model model) {
    Applicant applicant = applicantService.findById(id);
    model.addAttribute("a", applicant);
    return "admin/recruit/applicantView";
  }

  // 지원서 다운로드
  @GetMapping("/recruit/applicants/file/{id}")
  @ResponseBody
  public ResponseEntity<Resource> previewResume(@PathVariable Long id) throws IOException {

    Applicant a = applicantService.findById(id);

    Path filePath = Paths.get(uploadPath + a.getResumePath());
    Resource resource = new UrlResource(filePath.toUri());

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + a.getName() + "_resume.pdf\"")
            .body(resource);
  }

  // 관리자 메모/상태 저장
  @PostMapping("/recruit/applicants/update")
  public String updateApplicant(
          @RequestParam Long id,
          @RequestParam String status,
          @RequestParam(required = false) String adminMemo) {

    applicantService.updateMemoAndStatus(id, status, adminMemo);
    return "redirect:/admin/recruit/applicants";
  }

  /* ===========================
         채용 관리
    ============================ */
  @GetMapping("/recruit/manage")
  public String manage(Model model) {
    model.addAttribute("cfg", recruitService.get());
    return "admin/recruit/manage";
  }

  @PostMapping("/recruit/toggle")
  public String toggle(RedirectAttributes ra) {

    boolean nowOpen = recruitService.toggle();

    if (nowOpen) {
      ra.addFlashAttribute("msg", "채용 상태가 '모집중'으로 변경되었습니다.");
    } else {
      ra.addFlashAttribute("msg", "채용 상태가 '모집 마감'으로 변경되었습니다.");
    }
    return "redirect:/admin/recruit/manage";
  }

  @PostMapping("/recruit/save")
  public String save(RecruitConfig form, RedirectAttributes ra) {

    recruitService.save(form);

    ra.addFlashAttribute("msg", "직무별 안내 내용이 저장되었습니다.");

    return "redirect:/admin/recruit/manage";
  }

  /* ===========================
         Survey Meta (설문)
    ============================ */

  @GetMapping("/survey")
  public String surveyMetaList(Model model) {
    model.addAttribute("list", surveyService.findAllMeta());
    return "admin/survey/metaList";
  }

  @GetMapping("/survey/meta/new")
  public String newMeta(Model model) {
    model.addAttribute("meta", new SurveyMeta());
    return "admin/survey/metaEdit";
  }

  @PostMapping("/survey/meta/save")
  public String saveMeta(SurveyMeta meta) {
    surveyService.saveMeta(meta);
    return "redirect:/admin/survey";
  }

  @PostMapping("/survey/meta/delete/{id}")
  public String deleteMeta(@PathVariable Long id) {
    surveyService.deleteMeta(id);
    return "redirect:/admin/survey";
  }

  @PostMapping("/survey/meta/toggle/{id}")
  public String toggleMetaActive(@PathVariable Long id) {
    surveyService.toggleActive(id);
    return "redirect:/admin/survey";
  }

  /* ===========================
        Survey Question (문항)
  ============================ */
  @GetMapping("/survey/{surveyId}/questions")
  public String questionList(@PathVariable Long surveyId, Model model) {
    model.addAttribute("surveyId", surveyId);
    model.addAttribute("list", surveyService.findQuestionsByMeta(surveyId));
    return "admin/survey/questionList";
  }

  @GetMapping("/survey/{surveyId}/question/new")
  public String newQuestion(@PathVariable Long surveyId, Model model) {
    SurveyQuestion q = new SurveyQuestion();
    q.setSurveyId(surveyId);

    model.addAttribute("q", q);
    model.addAttribute("choices", List.of());
    return "admin/survey/questionEdit";
  }

  @GetMapping("/survey/{surveyId}/question/edit/{id}")
  public String editQuestion(@PathVariable Long surveyId,
                             @PathVariable Long id,
                             Model model) {

    SurveyQuestion q = surveyService.getQuestion(id);
    model.addAttribute("q", q);
    model.addAttribute("choices", surveyService.findChoices(id));
    return "admin/survey/questionEdit";
  }

  @PostMapping("/survey/{surveyId}/question/save")
  public String saveQuestion(
          @PathVariable Long surveyId,
          @ModelAttribute SurveyQuestion q,
          @RequestParam(required = false) List<String> choiceTexts
  ) {
    q.setSurveyId(surveyId);
    q = surveyService.saveQuestion(q);

    // TEXT형이면 선택지 삭제
    if (q.getQuestionType().name().equals("TEXT")) {
      surveyService.deleteChoicesByQuestion(q.getId());
      return "redirect:/admin/survey/" + surveyId + "/questions";
    }

    // 기존 선택지 삭제
    surveyService.deleteChoicesByQuestion(q.getId());

    // 새 선택지 저장
    if (choiceTexts != null) {
      int order = 0;
      for (String txt : choiceTexts) {
        if (txt == null || txt.trim().isEmpty()) continue;

        SurveyChoice c = new SurveyChoice();
        c.setQuestionId(q.getId());
        c.setChoiceText(txt.trim());
        c.setSortOrder(order++);
        surveyService.saveChoice(c);
      }
    }

    return "redirect:/admin/survey/" + surveyId + "/questions";
  }

  @PostMapping("/survey/{surveyId}/question/delete/{id}")
  public String deleteQuestion(
          @PathVariable Long surveyId,
          @PathVariable Long id
  ) {
    surveyService.deleteQuestion(id);
    return "redirect:/admin/survey/" + surveyId + "/questions";
  }

  /* ===========================
         Survey stats (통계)
    ============================ */
  @GetMapping("/survey/stats/{surveyId}")
  public String statsPage(@PathVariable Long surveyId, Model model) {

    List<QuestionStatsDto> statsList = surveyService.calcStats(surveyId);
    SurveyMeta meta = surveyService.getMeta(surveyId);

    model.addAttribute("surveyId", surveyId);
    model.addAttribute("statsList", statsList);
    model.addAttribute("surveyMeta", meta);

    return "admin/survey/surveyStats";
  }

  /* ===========================
      쇼핑몰 관리
   ============================ */
  @GetMapping("/dbShop/dbProductList")
  public String adminDbProductListGet(PageRequestDTO pageRequestDTO,
                                      @RequestParam(value = "statusMode", required = false) String statusMode,
                                      @RequestParam(value = "categories", required = false) List<DbProductCategory> categories,
                                      Model model) {

    model.addAttribute("result", adminService.getDbShopPage(pageRequestDTO, statusMode, categories));
    model.addAttribute("categories", categories);
    model.addAttribute("statusMode", statusMode);

    return "admin/dbShop/dbProductList";
  }

  @GetMapping("/dbShop/dbProductInput")
  public String adminDbProductInputGet() {
    return "admin/dbShop/dbProductInput";
  }

  @PostMapping("/dbShop/dbProductInput")
  public String adminDbProductInputPost(DbShopDTO dto) {
    adminService.setDbProductInput(dto);
    return "redirect:/admin/dbShop/dbProductList";
  }

  @GetMapping("/dbShop/dbProductUpdate")
  public String adminDbProductUpdateGet(Model model, @RequestParam("id") Long id) {
    DbShopDTO dto = adminService.getDbProductContent(id);
    model.addAttribute("dto", dto);
    return "admin/dbShop/dbProductUpdate";
  }

  @PostMapping("/dbShop/dbProductUpdate")
  public String adminDbProductUpdatePost(DbShopDTO dto) {
    adminService.setDbProductUpdate(dto);
    return "redirect:/admin/dbShop/dbProductList";
  }

  @PostMapping("/dbShop/dbProductDelete")
  @ResponseBody
  public ResponseEntity<String> adminDbProductDeletePost(@RequestParam("id") Long id) {
    log.info("상품 삭제 요청 ID : " + id);
    try {
      adminService.deleteDbProduct(id);
      return new ResponseEntity<>("삭제되었습니다.", HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>("삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PostMapping("/dbShop/stopSelling")
  public String stopSelling(@RequestParam("id") Long id) {
    adminService.stopSelling(id);
    return "redirect:/admin/dbShop/dbProductList";
  }

  @PostMapping("/dbShop/resell")
  public String resell(@RequestParam("id") Long id) {
    adminService.resell(id);
    return "redirect:/admin/dbShop/dbProductList";
  }

  @PostMapping("/dbShop/imageUpload")
  @ResponseBody
  public Map<String, Object> imageUpload(@RequestParam("upload") MultipartFile upload) {
    return adminService.uploadImage(upload);
  }

  @GetMapping("/dbShop/adminOrderList")
  public String adminOrderList(@ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) OrderStatus status,
                               Model model) {

    var result = orderService.getAdminOrderList(pageRequestDTO, startDate, endDate, status);

    model.addAttribute("result", result);
    model.addAttribute("orderStatusList", OrderStatus.values());
    model.addAttribute("startDate", startDate);
    model.addAttribute("endDate", endDate);
    model.addAttribute("searchStatus", status);

    return "admin/dbShop/adminOrderList";
  }

  @PostMapping("/order/updateStatus")
  public String updateOrderStatus(@RequestParam Long orderId,
                                  @RequestParam OrderStatus status,
                                  @RequestParam(defaultValue = "1") int page,
                                  @RequestParam(required = false) String type,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) OrderStatus searchStatus) {

    orderService.updateOrderStatus(orderId, status);

    return "redirect:/admin/dbShop/adminOrderList?page=" + page +
            "&type=" + (type != null ? type : "") +
            "&keyword=" + (keyword != null ? keyword : "") +
            "&startDate=" + (startDate != null ? startDate : "") +
            "&endDate=" + (endDate != null ? endDate : "") +
            "&searchStatus=" + (searchStatus != null ? searchStatus : "");
  }

  @PostMapping("/order/updateBatchStatus")
  @ResponseBody
  public ResponseEntity<String> updateBatchStatus(@RequestBody BatchStatusDTO dto) {
    try {
      orderService.updateOrdersStatus(dto.getOrderIds(), dto.getStatus());

      return new ResponseEntity<>("성공", HttpStatus.OK);
    } catch (Exception e) {
      log.error("일괄 변경 에러", e);
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
  }


  /* ===========================
     커뮤니티(게시판) 관리
  ============================ */
  @GetMapping("/report/list")
  public String reportList(Model model) {
    model.addAttribute("blindedBoards", adminService.getBlindedBoards());
    model.addAttribute("blindedBoardReplies", adminService.getBlindedBoardReplies());
    return "admin/report/reportList";
  }

  @PostMapping("/report/unblind")
  public String unblind(@RequestParam("type") String type, @RequestParam("id") Long id) {
    switch (type) {
      case "board" -> adminService.unblindBoard(id);
      case "boardReply" -> adminService.unblindBoardReply(id);
    }
    return "redirect:/admin/report/list";
  }

  @PostMapping("/report/delete")
  public String delete(@RequestParam("type") String type, @RequestParam("id") Long id) {
    switch (type) {
      case "board" -> adminService.deleteBoard(id);
      case "boardReply" -> adminService.deleteBoardReply(id);
    }
    return "redirect:/admin/report/list";
  }

  @GetMapping("/report/history/board/{boardId}")
  @ResponseBody
  public ResponseEntity<List<ReportHistoryDTO>> getBoardReportHistory(@PathVariable Long boardId) {
    List<ReportHistoryDTO> history = adminService.getBoardReportHistory(boardId);
    return ResponseEntity.ok(history);
  }

  @GetMapping("/report/history/reply/{replyId}")
  @ResponseBody
  public ResponseEntity<List<ReportHistoryDTO>> getBoardReplyReportHistory(@PathVariable Long replyId) {
    List<ReportHistoryDTO> history = adminService.getBoardReplyReportHistory(replyId);
    return ResponseEntity.ok(history);
  }

  /* ===========================
     관리자 메인페이지 관리
  ============================ */
  @GetMapping("/adminMain")
  public String adminMain(Model model) {

    AdminMainDTO dashboardData = adminService.getDashboardStatistics();

    model.addAttribute("newOrders", dashboardData.getNewOrderCount());
    model.addAttribute("todaySales", dashboardData.getTodaySales());
    model.addAttribute("monthSales", dashboardData.getMonthSales());
    model.addAttribute("claims", dashboardData.getClaimCount());

    model.addAttribute("recentOrders", dashboardData.getRecentOrders());
    model.addAttribute("preparingCount", dashboardData.getPreparingCount());
    model.addAttribute("communityBlindCount", dashboardData.getCommunityBlindCount());
    model.addAttribute("newMembers", dashboardData.getNewMemberCount());
    model.addAttribute("unansweredQna", dashboardData.getUnansweredQnaCount());

    return "admin/adminMain";
  }

}
