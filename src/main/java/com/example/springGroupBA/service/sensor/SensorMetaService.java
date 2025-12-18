package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.entity.sensor.SensorMeta;
import com.example.springGroupBA.repository.sensor.SensorMetaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SensorMetaService {

  private final SensorMetaRepository repo;

  @Value("${org.zerock.upload.path}")
  private String uploadPath;

  @Transactional
  public SensorMeta saveWithImage(SensorMeta meta, MultipartFile file) throws IOException {

    SensorMeta target;

    // ===== ADD / EDIT 분기 =====
    if (meta.getId() != null) {
      // 수정
      target = repo.findById(meta.getId()).orElseThrow();
    } else {
      // 추가
      target = new SensorMeta();
    }

    // ===== 필수 필드 세팅 =====
    target.setCode(meta.getCode());
    target.setName(meta.getName());
    target.setModel(meta.getModel());
    target.setSummary(meta.getSummary());
    target.setDescription(meta.getDescription());

    // ===== 이미지 업로드 =====
    if (file != null && !file.isEmpty()) {

      String folder = uploadPath + "sensor/";
      Files.createDirectories(Paths.get(folder));

      String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
      Path savePath = Paths.get(folder + filename);

      Files.write(savePath, file.getBytes());

      // 접근 가능한 URL 저장
      target.setImageUrl("/upload/sensor/" + filename);
    }

    // ===== 저장 =====
    return repo.save(target);
  }

  public List<SensorMeta> findAll() {
    return repo.findAll();
  }


  public SensorMeta findByCode(String code) {
    return repo.findByCode(code);
  }

  public SensorMeta save(SensorMeta meta) {
    return repo.save(meta);
  }

  public void delete(Long id) {
    repo.deleteById(id);
  }

  public Optional<SensorMeta> findById(Long id) {
    return repo.findById(id);
  }
}


