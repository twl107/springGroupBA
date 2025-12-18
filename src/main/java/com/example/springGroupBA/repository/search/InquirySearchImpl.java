package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.entity.inquiry.Inquiry;
import com.example.springGroupBA.entity.inquiry.QInquiry;
import com.example.springGroupBA.entity.member.QMember;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class InquirySearchImpl extends QuerydslRepositorySupport implements InquirySearch {

    public InquirySearchImpl() {
        super(Inquiry.class);
    }

    @Override
    public Page<Inquiry> searchInquiry(String email, InquiryStatus status, String type, String keyword, Pageable pageable) {

        QInquiry inquiry = QInquiry.inquiry;
        QMember member = QMember.member;

        JPQLQuery<Inquiry> query = from(inquiry);
        query.leftJoin(inquiry.member, member);

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if (email != null) {
            booleanBuilder.and(member.email.eq(email));
        }

        if (status != null) {
            booleanBuilder.and(inquiry.status.eq(status));
        }

        if (type != null && keyword != null && keyword.trim().length() > 0) {
            String[] typeArr = type.split("");
            for (String t : typeArr) {
                switch (t) {
                    case "t":
                        conditionBuilder.or(inquiry.title.contains(keyword));
                        break;
                    case "c":
                        conditionBuilder.or(inquiry.content.contains(keyword));
                        break;
                    case "w":
                        conditionBuilder.or(member.name.contains(keyword));
                        break;
                }
            }
            booleanBuilder.and(conditionBuilder);
        }

        booleanBuilder.and(inquiry.id.gt(0L));

        query.where(booleanBuilder);

        this.getQuerydsl().applyPagination(pageable, query);

        List<Inquiry> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }
}
