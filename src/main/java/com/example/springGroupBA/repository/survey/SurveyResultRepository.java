package com.example.springGroupBA.repository.survey;

import com.example.springGroupBA.entity.survey.SurveyResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyResultRepository extends JpaRepository<SurveyResult, Long> {

  List<SurveyResult> findBySurveyId(Long metaId);

  boolean existsByMemberIdAndSurveyId(Long memberId, Long surveyId);
}
