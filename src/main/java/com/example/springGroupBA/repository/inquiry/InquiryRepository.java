package com.example.springGroupBA.repository.inquiry;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.entity.inquiry.Inquiry;
import com.example.springGroupBA.repository.search.InquirySearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long>, InquirySearch {

    List<Inquiry> findByMember_EmailOrderByRegDateDesc(String email);

    long countByStatus(InquiryStatus status);
}
