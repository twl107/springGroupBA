package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.ApplyForm;
import com.example.springGroupBA.service.ApplicantService;
import com.example.springGroupBA.service.RecruitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/recruit")
public class RecruitController {

  private final ApplicantService applicantService;
  private final RecruitService recruitService;


  /* ================================
     1) 채용 프로세스 페이지
   ================================ */
  @GetMapping("/process")
  public String processPage() {
    return "recruit/process";  // templates/recruit/process.html
  }


  /* ================================
     2) 복지 페이지
   ================================ */
  @GetMapping("/welfare")
  public String welfarePage() {
    return "recruit/welfare";
  }


  /* ================================
     3) 이력서 양식 다운로드 페이지
   ================================ */
  @GetMapping("/template")
  public String templatePage() {
    return "recruit/template";
  }


  /* ================================
     4) 지원하기 폼 (GET)
   ================================ */
  @GetMapping("/apply")
  public String applyLanding(Model model) {

    boolean recruitOpen = recruitService.isRecruitOpen();
    model.addAttribute("recruitOpen", recruitOpen);

    // ★ cfg 전달 추가
    model.addAttribute("cfg", recruitService.get());

    return "recruit/applyLanding";
  }

  @GetMapping("/recruit")
  public String landing(Model model) {
    model.addAttribute("cfg", recruitService.get());
    return "recruit/landing";
  }

  @GetMapping("/applyForm")
  public String applyForm(Model model) {
    model.addAttribute("form", new ApplyForm());
    return "recruit/applyForm";
  }


  /* ================================
     5) 지원하기 처리 (POST)
   ================================ */
  @PostMapping("/apply")
  public String applySubmit(ApplyForm form,
                            @RequestParam("resume") MultipartFile resumeFile,
                            Model model) {

    try {
      applicantService.saveApplicant(form, resumeFile);

      model.addAttribute("msg", "지원이 정상적으로 완료되었습니다.");
      model.addAttribute("url", "/");

    } catch (Exception e) {
      e.printStackTrace();

      model.addAttribute("msg", "지원 중 예상치 못한 오류가 발생했습니다.");
      model.addAttribute("url", "/");
    }

    return "common/message";  // alert 후 redirect 하는 페이지
  }

}
