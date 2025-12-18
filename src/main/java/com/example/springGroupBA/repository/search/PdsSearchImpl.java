package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.entity.member.QMember;
import com.example.springGroupBA.entity.pds.Pds;
import com.example.springGroupBA.entity.pds.QPds;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class PdsSearchImpl extends QuerydslRepositorySupport implements PdsSearch {

    public PdsSearchImpl() {
        super(Pds.class);
    }

    @Override
    public Page<Pds> searchPage(String type, String keyword, Pageable pageable) {

        QPds pds = QPds.pds;
        QMember member = QMember.member;

        JPQLQuery<Pds> query = from(pds);
        query.leftJoin(pds.member, member);

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if(type != null && keyword != null && keyword.trim().length() > 0) {
            String[] typeArr = type.split("");

            for (String t : typeArr) {
                switch (t) {
                    case "t" :
                        conditionBuilder.or(pds.title.contains(keyword));
                        break;
                    case  "c" :
                        conditionBuilder.or(pds.content.contains(keyword));
                        break;
                    case  "w" :
                        conditionBuilder.or(member.name.contains(keyword));
                        break;
                }
            }
            booleanBuilder.and(conditionBuilder);
        }

        booleanBuilder.and(pds.id.gt(0L));

        query.where(booleanBuilder);

        this.getQuerydsl().applyPagination(pageable, query);

        List<Pds> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }
}
