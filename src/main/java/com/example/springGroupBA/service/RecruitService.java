package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.recruit.RecruitConfig;
import com.example.springGroupBA.repository.recruit.RecruitConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecruitService {

  private final RecruitConfigRepository repo;

  public RecruitConfig get() {
    return repo.findById(1L).orElseGet(() -> {
      RecruitConfig defaultConfig = RecruitConfig.builder()
              .id(1L)
              .open(false)  // default 상태
              .build();
      return repo.save(defaultConfig);
    });
  }

  public boolean toggle() {
    RecruitConfig cfg = get();
    cfg.setOpen(!cfg.isOpen());
    repo.save(cfg);

    return cfg.isOpen(); // 변경된 상태 반환
  }

  public void save(RecruitConfig cfg) {
    cfg.setId(1L);
    repo.save(cfg);
  }

  public boolean isRecruitOpen() {
    return get().isOpen();
  }
}

