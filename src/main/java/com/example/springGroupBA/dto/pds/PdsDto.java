package com.example.springGroupBA.dto.pds;

import com.example.springGroupBA.entity.pds.Pds;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdsDto {

    private Long id;

    private String mid;
    private String writerName;
    private String writerEmail;

    private String title;
    private String content;
    private String openSw;

    private int readNum;
    private int downNum;
    private int good;
    private int bad;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    private int replyCount;
    private int fileCount;

    @Builder.Default
    private List<PdsFileDto> files = new ArrayList<>();

    public static PdsDto entityToDto(Pds pds) {
        List<PdsFileDto> fileDtos = pds.getPdsFiles().stream()
                .map(PdsFileDto::entityToDto)
                .collect(Collectors.toList());

        return PdsDto.builder()
                .id(pds.getId())
                .mid(pds.getMember().getMid())
                .writerName(pds.getMember().getName())
                .writerEmail(pds.getMember().getEmail())
                .title(pds.getTitle())
                .content(pds.getContent())
                .openSw(pds.getOpenSw())
                .readNum(pds.getReadNum())
                .downNum(pds.getDownNum())
                .good(pds.getGood())
                .bad(pds.getBad())
                .regDate(pds.getRegDate())
                .modDate(pds.getModDate())
                .replyCount(pds.getPdsReplies().size())
                .fileCount(pds.getPdsFiles().size())
                .files(fileDtos)
                .build();
    }
}