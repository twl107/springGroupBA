package com.example.springGroupBA.dto.survey;

import com.example.springGroupBA.entity.survey.SurveyChoice;
import com.example.springGroupBA.entity.survey.SurveyQuestion;
import lombok.Data;

import java.util.List;

@Data
public class SurveyViewDto {
  private SurveyQuestion question;
  private List<SurveyChoice> choices;
}
