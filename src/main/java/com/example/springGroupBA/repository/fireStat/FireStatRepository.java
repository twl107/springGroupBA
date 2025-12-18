package com.example.springGroupBA.repository.fireStat;

import com.example.springGroupBA.entity.fireStat.FireStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FireStatRepository extends JpaRepository<FireStat, Long> {

    List<FireStat> findAllByYearOrderByLocationAsc(Integer year);

    boolean existsByYearAndLocation(Integer year, String location);

    boolean existsByYear(Integer year);

    @Modifying
    @Transactional
    void deleteByYear(Integer year);

}
