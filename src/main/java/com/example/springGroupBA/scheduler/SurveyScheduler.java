package com.example.springGroupBA.scheduler;

import com.example.springGroupBA.entity.survey.SurveyMeta;
import com.example.springGroupBA.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyScheduler {

  private final SurveyService surveyService;

  @Scheduled(cron = "0 0 3 * * *")
  public void autoDisableExpiredSurveys() {

    List<SurveyMeta> list = surveyService.findAllMeta();

    for (SurveyMeta m : list) {
      if (m.getActive().equals("Y")
              && m.getEndDate() != null
              && m.getEndDate().isBefore(LocalDateTime.now())) {

        m.setActive("N");
        surveyService.saveMeta(m);
      }
    }
  }
}
