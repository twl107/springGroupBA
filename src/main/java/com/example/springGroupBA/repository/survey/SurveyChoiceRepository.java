package com.example.springGroupBA.repository.survey;

import com.example.springGroupBA.entity.survey.SurveyChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyChoiceRepository extends JpaRepository<SurveyChoice, Long> {

  // 특정 질문의 보기 목록
  List<SurveyChoice> findAllByQuestionIdOrderBySortOrderAsc(Long questionId);

  // 특정 질문 보기 전체 삭제
  void deleteByQuestionId(Long questionId);
}
