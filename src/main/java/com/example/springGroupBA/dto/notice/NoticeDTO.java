package com.example.springGroupBA.dto.notice;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDTO {

    private Long id;
    private String title;
    private String content;

    private String mid;
    private String email;
    private String writerName;

    private String openSw;
    private int readNum;
    private boolean fixed;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    @Builder.Default
    private List<NoticeFileDTO> files = new ArrayList<>();

    private int fileCount;

}
