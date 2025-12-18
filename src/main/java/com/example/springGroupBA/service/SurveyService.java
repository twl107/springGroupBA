package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.SurveyQuestionType;
import com.example.springGroupBA.dto.survey.QuestionStatsDto;
import com.example.springGroupBA.entity.survey.SurveyChoice;
import com.example.springGroupBA.entity.survey.SurveyMeta;
import com.example.springGroupBA.entity.survey.SurveyQuestion;
import com.example.springGroupBA.entity.survey.SurveyResult;
import com.example.springGroupBA.repository.survey.SurveyChoiceRepository;
import com.example.springGroupBA.repository.survey.SurveyMetaRepository;
import com.example.springGroupBA.repository.survey.SurveyQuestionRepository;
import com.example.springGroupBA.repository.survey.SurveyResultRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SurveyService {

  private final SurveyMetaRepository metaRepo;
  private final SurveyQuestionRepository qRepo;
  private final SurveyChoiceRepository choiceRepo;
  private final SurveyResultRepository resultRepo;

  /* =================== CHOICE =================== */

  // 보기 목록
  public List<SurveyChoice> findChoices(Long questionId) {
    return choiceRepo.findAllByQuestionIdOrderBySortOrderAsc(questionId);
  }

  // 보기 저장
  public SurveyChoice saveChoice(SurveyChoice c) {
    return choiceRepo.save(c);
  }

  // 보기 삭제 (id 단건)
  public void deleteChoice(Long id) {
    choiceRepo.deleteById(id);
  }

  // 보기 전체 삭제 (질문 수정시 초기화용)
  @Transactional
  public void deleteChoicesByQuestion(Long questionId) {
    choiceRepo.deleteByQuestionId(questionId);
  }

  /* =================== META =================== */

  public List<SurveyMeta> findAllMeta() {
    return metaRepo.findAll();
  }

  public SurveyMeta saveMeta(SurveyMeta meta) {

    // 신규 생성
    if (meta.getId() == null) {

      // 생성일 자동 부여
      meta.setStartDate(LocalDateTime.now());

      // 종료일이 없으면 자동 30일 후로
      if (meta.getEndDate() == null) {
        meta.setEndDate(LocalDateTime.now().plusDays(30));
      }

      // 신규는 기본 활성
      meta.setActive("Y");

    } else {

      // 기존 설문 수정일경우 → 종료일이 지났으면 비활성 처리
      if (meta.getEndDate() != null && meta.getEndDate().isBefore(LocalDateTime.now())) {
        meta.setActive("N");
      }
    }

    return metaRepo.save(meta);
  }

  public SurveyMeta getMeta(Long id) {
    return metaRepo.findById(id).orElseThrow();
  }

  public void deleteMeta(Long id) {
    metaRepo.deleteById(id);
  }


  /* =================== QUESTION =================== */

  public List<SurveyQuestion> findQuestionsByMeta(Long metaId) {
    return qRepo.findAllBySurveyIdOrderBySortOrderAsc(metaId);
  }

  public SurveyQuestion saveQuestion(SurveyQuestion q) {
    return qRepo.save(q);
  }

  public void deleteQuestion(Long id) {
    qRepo.deleteById(id);
  }

  public SurveyQuestion getQuestion(Long id) {
    return qRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("질문을 찾을 수 없습니다. id=" + id));
  }


  /* =================== RESULT =================== */

  public void saveResult(SurveyResult result) {
    resultRepo.save(result);
  }

  public boolean hasSubmitted(Long memberId, Long surveyId) {
    return resultRepo.existsByMemberIdAndSurveyId(memberId, surveyId);
  }

  public List<SurveyResult> findAllResults() {
    return resultRepo.findAll();
  }

  // 특정 설문 결과만 조회 (추가)
  public List<SurveyResult> findResultsByMeta(Long metaId) {
    return resultRepo.findBySurveyId(metaId);
  }

  public void toggleActive(Long id) {
    SurveyMeta m = getMeta(id);
    m.setActive(m.getActive().equals("Y") ? "N" : "Y");
    m.setUpdatedAt(LocalDateTime.now());
    saveMeta(m);
  }

  public List<SurveyMeta> findActiveMeta() {
    return metaRepo.findByActive("Y");
  }

  /* =================== STATICS =================== */
  public List<QuestionStatsDto> calcStats(Long surveyId) {

    List<SurveyResult> results = resultRepo.findBySurveyId(surveyId);

    // 1) QuestionId -> 통계 DTO 매핑용 저장소
    Map<Long, QuestionStatsDto> statsMap = new LinkedHashMap<>();

    // 2) 설문 메타(문항 리스트) 조회
    List<SurveyQuestion> questions = qRepo.findBySurveyIdOrderBySortOrderAsc(surveyId);

    // 2-1) 질문별 DTO 미리 세팅
    for (SurveyQuestion q : questions) {
      QuestionStatsDto dto = new QuestionStatsDto();
      dto.setQuestionId(q.getId());
      dto.setQuestionText(q.getQuestionText());
      dto.setType(q.getQuestionType());
      dto.setTotalResponses(0);

      statsMap.put(q.getId(), dto);
    }

    // 3) 응답 돌면서 JSON 파싱 처리
    for (SurveyResult r : results) {
      JSONObject json = (JSONObject) JSONValue.parse(r.getAnswersJson());

      for (SurveyQuestion q : questions) {
        String key = "q" + q.getId();
        Object value = json.get(key);

        if (value == null) continue;

        QuestionStatsDto dto = statsMap.get(q.getId());
        dto.setTotalResponses(dto.getTotalResponses() + 1);

        // 4) 타입별 누적 처리
        switch (q.getQuestionType()) {

          case SINGLE:
          case SELECT: // SELECT도 SINGLE처럼 처리
            String sVal = value.toString();
            dto.getCountMap().put(sVal, dto.getCountMap().getOrDefault(sVal, 0) + 1);
            break;

          case MULTI:
            String[] arr = value.toString().split(",");
            for (String item : arr) {
              item = item.trim();
              dto.getCountMap().put(item, dto.getCountMap().getOrDefault(item, 0) + 1);
            }
            break;

          case TEXT:
            dto.getTextResponses().add(value.toString());
            break;

          case RATING:
            int num = Integer.parseInt(value.toString());
            if (dto.getMin() == null || num < dto.getMin()) dto.setMin(num);
            if (dto.getMax() == null || num > dto.getMax()) dto.setMax(num);
            dto.getCountMap().put("sum", dto.getCountMap().getOrDefault("sum", 0) + num);
            break;
        }

      }
    }

    // 5) 통계 후처리 (비율, 평균 계산)
    for (QuestionStatsDto dto : statsMap.values()) {

      if (dto.getType() == SurveyQuestionType.SINGLE
              || dto.getType() == SurveyQuestionType.MULTI
              || dto.getType() == SurveyQuestionType.SELECT) {

        int total = dto.getTotalResponses();
        for (String key : dto.getCountMap().keySet()) {
          int cnt = dto.getCountMap().get(key);
          double pct = (cnt * 100.0) / total;
          dto.getPercentMap().put(key, Math.round(pct * 10) / 10.0);
        }
      }

      if (dto.getType() == SurveyQuestionType.RATING) {
        int total = dto.getTotalResponses();
        int sum = dto.getCountMap().getOrDefault("sum", 0);
        dto.setAvg(Math.round((sum * 10.0 / total)) / 10.0);
      }
    }

    return new ArrayList<>(statsMap.values());
  }

  public SurveyMeta getSurveyMeta(Long surveyId) {
    return metaRepo.findById(surveyId)
            .orElseThrow(() -> new RuntimeException("Survey not found"));
  }
}
