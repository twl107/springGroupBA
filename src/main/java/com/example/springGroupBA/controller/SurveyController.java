package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.survey.SurveyViewDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.survey.SurveyMeta;
import com.example.springGroupBA.entity.survey.SurveyQuestion;
import com.example.springGroupBA.entity.survey.SurveyResult;
import com.example.springGroupBA.service.MemberService;
import com.example.springGroupBA.service.SurveyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class SurveyController {

  private final SurveyService surveyService;
  private final MemberService memberService;

  @GetMapping("/survey/start/{surveyId}")
  public String startSurvey(
          @PathVariable Long surveyId,
          Model model
  ) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    Member member = memberService.getMemberByEmail(email).orElseThrow();

    // 🔍 이 설문을 이미 제출했는가? (설문별 검사)
    if (surveyService.hasSubmitted(member.getId(), surveyId)) {
      return "survey/surveyAlready";
    }

    // 🔍 설문 정보 가져오기
    SurveyMeta meta = surveyService.getMeta(surveyId);

    List<SurveyViewDto> viewList = new ArrayList<>();

    for (SurveyQuestion q : surveyService.findQuestionsByMeta(surveyId)) {
      SurveyViewDto dto = new SurveyViewDto();
      dto.setQuestion(q);
      dto.setChoices(surveyService.findChoices(q.getId()));
      viewList.add(dto);
    }

    // 🔍 모델에 전달
    model.addAttribute("meta", meta);
    model.addAttribute("formList", viewList);

    return "survey/surveyForm";
  }


  /*설문 문항이 정적으로 고정되지 않도록
  문항 ID 기반 JSON 구조로 설계했습니다.
  문항 수가 증가하거나 변경되더라도
  DB 구조 변경 없이 통계 분석이 가능합니다.*/

  @PostMapping("/survey/submit")
  public String submitSurvey(
          @RequestParam("surveyId") Long surveyId,
          HttpServletRequest req
  ) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String email = auth.getName();
    Member member = memberService.getMemberByEmail(email).orElseThrow();

    // 🔍 이 설문을 이미 제출한 경우 (설문별 검사)
    if (surveyService.hasSubmitted(member.getId(), surveyId)) {
      return "survey/surveyAlready";
    }

    Map<String, String[]> paramMap = req.getParameterMap();
    JSONObject json = new JSONObject();

    // 🔍 응답 JSON 수집 (질문 prefix q시)
    for (String key : paramMap.keySet()) {
      if (key.startsWith("q")) {

        // 체크박스 등 multiple 선택
        if (paramMap.get(key).length > 1) {
          String joined = String.join(",", paramMap.get(key));
          json.put(key, joined);
        } else { // 단일 값
          json.put(key, paramMap.get(key)[0]);
        }
      }
    }

    // 🔹 저장
    SurveyResult result = new SurveyResult();
    result.setSurveyId(surveyId);      // ★ 설문별 저장
    result.setMemberId(member.getId());
    result.setAnswersJson(json.toJSONString());

    surveyService.saveResult(result);

    return "survey/surveyThankYou";
  }
}

