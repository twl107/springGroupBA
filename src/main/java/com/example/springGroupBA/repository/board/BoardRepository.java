package com.example.springGroupBA.repository.board;

import com.example.springGroupBA.entity.board.Board;
import com.example.springGroupBA.repository.search.BoardSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long>, BoardSearch {

    @Modifying
    @Query("update Board b set b.readNum = b.readNum + 1 where b.id = :id")
    void updateReadCount(@Param("id") Long id);

    @Modifying
    @Query("update Board b set b.good = b.good + 1 where b.id = :id")
    void updateGoodCount(@Param("id") Long id);

    @Modifying
    @Query("update Board b set b.bad = b.bad + 1 where b.id = :id")
    void updateBadCount(@Param("id") Long id);

    List<Board> findByIsBlindTrueOrderByRegDateDesc();

}