package com.example.springGroupBA.dto.board;

import com.example.springGroupBA.entity.board.BoardReply;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardReplyDTO {

    private Long id;
    private Long boardId;

    private String mid;
    private String writerName;
    private String writerEmail;
    private String photoName;

    private String content;
    private boolean isRemoved;
    private boolean isBlind;

    private Long parentId;
    private int depth;
    private int report;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    public static BoardReplyDTO entityToDto(BoardReply reply) {

        String content = reply.isRemoved() ? "삭제된 댓글입니다." : reply.getContent();
        String writerName = reply.isRemoved() ? "(알 수 없음)" : reply.getMember().getName();

        return BoardReplyDTO.builder()
                .id(reply.getId())
                .boardId(reply.getBoard().getId())
                .mid(reply.getMember().getMid())
                .writerName(writerName)
                .writerEmail(reply.getMember().getEmail())
                .photoName(reply.getMember().getPhotoName())
                .content(content)
                .parentId(reply.getParent() != null ? reply.getParent().getId() : null)
                .depth(reply.getDepth())
                .isRemoved(reply.isRemoved())
                .report(reply.getReport())
                .isBlind(reply.isBlind())
                .regDate(reply.getRegDate())
                .modDate(reply.getModDate())
                .build();
    }
}
