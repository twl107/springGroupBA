package com.example.springGroupBA.dto.board;

import com.example.springGroupBA.entity.board.Board;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BoardDTO {

    private Long id;
    private String title;
    private String content;

    private String mid;
    private String email;
    private String writerName;

    private String openSw;
    private Boolean isBlind;
    private int readNum;
    private int good;
    private int bad;
    private int reportCount;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    @Builder.Default
    private List<BoardFileDTO> files = new ArrayList<>();

    private int fileCount;
    private int replyCount;

    public static BoardDTO entityToDto(Board board) {
        List<BoardFileDTO> fileDTOs = board.getFiles().stream().map(BoardFileDTO::new).collect(Collectors.toList());

        return BoardDTO.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .mid(board.getMember().getMid())
                .email(board.getMember().getEmail())
                .writerName(board.getMember().getName())
                .openSw(board.getOpenSw())
                .isBlind(board.isBlind())
                .readNum(board.getReadNum())
                .good(board.getGood())
                .bad(board.getBad())
                .reportCount(board.getReportCount())
                .regDate(board.getRegDate())
                .modDate(board.getModDate())
                .files(fileDTOs)
                .fileCount(fileDTOs.size())
                .replyCount(board.getReplies() != null ? board.getReplies().size() : 0)
                .build();
    }
}