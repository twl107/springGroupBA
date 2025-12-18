package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.entity.board.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BoardSearch {
    Page<Board> searchPage(String type, String keyword, Pageable pageable);
}