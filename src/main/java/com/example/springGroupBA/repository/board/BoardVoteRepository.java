package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.BoardVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {

    boolean existsByBoardIdAndMid(Long boardId, String mid);

    void deleteByBoardId(Long boardId);
}