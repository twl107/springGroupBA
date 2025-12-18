package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.BoardReply;
import com.example.springGroupBA.entity.board.BoardReplyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardReplyReportRepository extends JpaRepository<BoardReplyReport, Long> {

    List<BoardReplyReport> findByBoardReply(BoardReply boardReply);
}
