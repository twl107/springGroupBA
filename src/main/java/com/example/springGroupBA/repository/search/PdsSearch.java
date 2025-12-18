package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.entity.pds.Pds;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PdsSearch {
    Page<Pds> searchPage(String type, String keyword, Pageable pageable);
}
