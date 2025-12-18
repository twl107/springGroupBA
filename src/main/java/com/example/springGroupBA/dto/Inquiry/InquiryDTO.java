package com.example.springGroupBA.dto.Inquiry;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.entity.inquiry.Inquiry;
import com.example.springGroupBA.entity.member.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryDTO {

    private Long id;
    private String title;
    private String content;
    private InquiryStatus status;
    private String answer;
    private String writerEmail;
    private String writerName;
    private LocalDateTime regDate;
    private LocalDateTime modDate;
    private MultipartFile file;
    private String originalFileName;
    private String saveFileName;

    public static InquiryDTO entityToDto(Inquiry inquiry) {
        return InquiryDTO.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .answer(inquiry.getAnswer())
                .writerName(inquiry.getMember().getName())
                .regDate(inquiry.getRegDate())
                .modDate(inquiry.getModDate())
                .originalFileName(inquiry.getOriginalFileName())
                .saveFileName(inquiry.getSaveFileName())
                .build();
    }

    public Inquiry dtoToEntity(Member member) {
        return Inquiry.builder()
                .title(this.title)
                .content(this.content)
                .member(member)
                .status(InquiryStatus.WAITING)
                .build();
    }
}
