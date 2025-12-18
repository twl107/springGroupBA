package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.entity.inquiry.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InquirySearch {

    Page<Inquiry> searchInquiry(String email, InquiryStatus status, String type, String keyword, Pageable pageable);
}
