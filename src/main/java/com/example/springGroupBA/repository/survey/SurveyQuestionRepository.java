package com.example.springGroupBA.repository.survey;

import com.example.springGroupBA.entity.survey.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
  List<SurveyQuestion> findAllBySurveyIdOrderBySortOrderAsc(Long surveyId);

  List<SurveyQuestion> findBySurveyIdOrderBySortOrderAsc(Long surveyId);
}
