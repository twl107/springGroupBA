package com.example.springGroupBA.repository.survey;

import com.example.springGroupBA.entity.survey.SurveyMeta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SurveyMetaRepository extends JpaRepository<SurveyMeta, Long> {
  List<SurveyMeta> findByActive(String active);
}
