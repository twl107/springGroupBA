package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.Board;
import com.example.springGroupBA.entity.board.BoardReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardReportRepository extends JpaRepository<BoardReport, Long> {

    List<BoardReport> findByBoard(Board board);
}
