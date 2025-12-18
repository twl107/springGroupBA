package com.example.springGroupBA.repository.recruit;

import com.example.springGroupBA.entity.recruit.Applicant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
  Page<Applicant> findByStatus(String status, Pageable pageable);

  Page<Applicant> findByPosition(String position, Pageable pageable);

  Page<Applicant> findByStatusAndPosition(String status, String position, Pageable pageable);
}

