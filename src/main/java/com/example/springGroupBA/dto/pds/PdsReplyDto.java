package com.example.springGroupBA.dto.pds;

import com.example.springGroupBA.entity.pds.PdsReply;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdsReplyDto {

    private Long id;
    private Long pdsId;

    private String mid;
    private String writerName;
    private String writerEmail;
    private String content;
    private String photoName;

    private LocalDateTime regDate;
    private LocalDateTime modDate;

    private Long parentId;
    private int depth;

    private boolean isRemoved;

    public static PdsReplyDto entityToDto(PdsReply pdsReply) {
        return PdsReplyDto.builder()
                .id(pdsReply.getId())
                .pdsId(pdsReply.getPds().getId())
                .mid(pdsReply.getMember().getMid())
                .writerName(pdsReply.getMember().getNickName())
                .writerEmail(pdsReply.getMember().getEmail())
                .photoName(pdsReply.getMember().getPhotoName())
                .content(pdsReply.getContent())
                .regDate(pdsReply.getRegDate())
                .modDate(pdsReply.getModDate())
                .parentId(pdsReply.getParent() != null ? pdsReply.getParent().getId() : null)
                .isRemoved(pdsReply.isRemoved())
                .build();
    }
}