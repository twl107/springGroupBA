package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.BoardFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardFileRepository extends JpaRepository<BoardFile, Long> {
}
