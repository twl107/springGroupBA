package com.example.springGroupBA.repository.notice;

import com.example.springGroupBA.entity.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    @EntityGraph(attributePaths = {"member"})
    @Query("select n from Notice n order by n.fixed desc, n.id desc")
    Page<Notice> findAllDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"member"})
    @Query("select n  from Notice n where n.title like %:keyword% or n.content like %:keyword% order by n.fixed desc, n.id desc")
    Page<Notice> findByKeyword(@Param("keyword") String keyword, Pageable pageable);

}
