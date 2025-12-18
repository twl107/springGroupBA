package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.BoardReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardReplyRepository extends JpaRepository<BoardReply, Long> {

    List<BoardReply> findAllByBoardIdOrderByRegDateAsc(Long boardId);

    int countByParentId(Long parentId);

    List<BoardReply> findByIsBlindTrueOrderByRegDateDesc();

}
