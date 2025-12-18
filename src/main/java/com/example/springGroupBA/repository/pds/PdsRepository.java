package com.example.springGroupBA.repository.pds;

import com.example.springGroupBA.entity.pds.Pds;
import com.example.springGroupBA.repository.search.PdsSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PdsRepository extends JpaRepository<Pds, Long>, PdsSearch {

    @Modifying
    @Query("update Pds p set p.readNum = p.readNum + 1 where p.id = :id")
    void updateReadCount(@Param("id") Long id);

    @Modifying
    @Query("update Pds p set p.good = p.good + 1 where p.id = :id")
    void updateGoodCount(@Param("id") Long id);

    @Modifying
    @Query("update Pds p set p.bad = p.bad + 1 where p.id = :id")
    void updateBadCount(@Param("id") Long id);

    List<Pds> findByIsBlindTrueOrderByRegDateDesc();


}
