package com.example.springGroupBA.repository.search;

import com.example.springGroupBA.entity.board.Board;
import com.example.springGroupBA.entity.board.QBoard;
import com.example.springGroupBA.entity.member.QMember;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;

import java.util.List;

public class BoardSearchImpl extends QuerydslRepositorySupport implements BoardSearch {

    public BoardSearchImpl() {
        super(Board.class);
    }

    @Override
    public Page<Board> searchPage(String type, String keyword, Pageable pageable) {

        QBoard board = QBoard.board;
        QMember member = QMember.member;

        JPQLQuery<Board> query = from(board);

        query.leftJoin(board.member, member);

        BooleanBuilder booleanBuilder = new BooleanBuilder();
        BooleanBuilder conditionBuilder = new BooleanBuilder();

        if(type != null && keyword != null && keyword.trim().length() > 0) {
            String[] typeArr = type.split("");

            for (String t : typeArr) {
                switch (t) {
                    case "t":
                        conditionBuilder.or(board.title.contains(keyword));
                        break;
                    case "c":
                        conditionBuilder.or(board.content.contains(keyword));
                        break;
                    case "w":
                        conditionBuilder.or(member.name.contains(keyword));
                        break;
                }
            }
            booleanBuilder.and(conditionBuilder);
        }

        booleanBuilder.and(board.id.gt(0L));

        query.where(booleanBuilder);

        this.getQuerydsl().applyPagination(pageable, query);

        List<Board> list = query.fetch();
        long count = query.fetchCount();

        return new PageImpl<>(list, pageable, count);
    }
}