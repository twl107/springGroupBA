package com.example.springGroupBA.repository.pds;

import com.example.springGroupBA.entity.pds.Pds;
import com.example.springGroupBA.entity.pds.PdsReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PdsReplyRepository extends JpaRepository<PdsReply, Long> {

    List<PdsReply> findByPdsOrderByRegDateAsc(Pds pds);

    int countByParentId(Long parentId);

    List<PdsReply> findByIsBlindTrueOrderByRegDateDesc();

}
