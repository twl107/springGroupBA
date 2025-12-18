package com.example.springGroupBA.service;

import com.example.springGroupBA.common.ProjectProvide;
import com.example.springGroupBA.dto.ApplyForm;
import com.example.springGroupBA.entity.recruit.Applicant;
import com.example.springGroupBA.repository.recruit.ApplicantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicantService {

  @Value("${org.zerock.upload.path}")
  private String uploadPath;   // C:/upload/

  private final ApplicantRepository applicantRepository;
  private final ProjectProvide projectProvide;

  public void saveApplicant(ApplyForm form, MultipartFile file) throws IOException {

    // 실제 저장 경로
    String resumeDir = uploadPath + "resume/";

    File dir = new File(resumeDir);
    if (!dir.exists()) dir.mkdirs();

    String storedFileName = null;

    // ------ 파일 저장 ------
    if (!file.isEmpty()) {

      String originalName = file.getOriginalFilename();
      String ext = StringUtils.getFilenameExtension(originalName);

      if (ext == null) ext = "dat";

      storedFileName = UUID.randomUUID() + "." + ext;

      File saveFile = new File(resumeDir + storedFileName);
      file.transferTo(saveFile);

      log.info("Resume saved: {}", saveFile.getAbsolutePath());
    }

    // ------ DB 저장 ------
    Applicant applicant = Applicant.builder()
            .name(form.getName())
            .email(form.getEmail())
            .phone(form.getPhone())
            .intro(form.getIntro())
            .position(form.getPosition())
            .resumePath("resume/" + storedFileName)
            .status("검토중")   // 👈 추가!
            .createdAt(LocalDateTime.now())
            .build();

    applicantRepository.save(applicant);

    log.info("Applicant saved in DB: {}", applicant.getId());

    // ------ 지원 완료 메일 발송 ------
    try {
      projectProvide.sendApplyMail(applicant.getEmail(),
              applicant.getName(),
              applicant.getPosition());
    } catch (Exception e) {
      log.error("메일 발송 오류: {}", e.getMessage());
    }
  }

  public Page<Applicant> findAll(Pageable pageable) {
    return applicantRepository.findAll(pageable);
  }

  public Applicant findById(Long id) {
    return applicantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("지원자 정보를 찾을 수 없습니다."));
  }

  public void delete(Long id) {
    applicantRepository.deleteById(id);
  }

  public void deleteAll(List<Long> ids) {
    applicantRepository.deleteAllById(ids);
  }

  public Page<Applicant> findByStatus(String status, Pageable pageable) {
    return applicantRepository.findByStatus(status, pageable);
  }

  public Page<Applicant> findByPosition(String position, Pageable pageable) {
    return applicantRepository.findByPosition(position, pageable);
  }

  public Page<Applicant> findByStatusAndPosition(String status, String position, Pageable pageable) {
    return applicantRepository.findByStatusAndPosition(status, position, pageable);
  }

  @Transactional
  public void updateMemoAndStatus(Long id, String status, String adminMemo) {

    Applicant a = applicantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Applicant not found"));

    a.setStatus(status);
    a.setAdminMemo(adminMemo);

    applicantRepository.save(a);
  }

}
