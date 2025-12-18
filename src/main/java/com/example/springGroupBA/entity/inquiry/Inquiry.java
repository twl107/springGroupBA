package com.example.springGroupBA.entity.inquiry;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.entity.BaseEntity;
import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inquiry")
public class Inquiry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryStatus status;

    @Column(columnDefinition = "TEXT")
    private String answer;

    private String originalFileName;
    private String saveFileName;

    public void updateInquiry(String title, String content, String originalFileName, String saveFileName) {
        this.title = title;
        this.content = content;

        if (originalFileName != null && saveFileName != null) {
            this.originalFileName = originalFileName;
            this.saveFileName = saveFileName;
        }
    }

    public void answerInquiry(String answer) {
        this.answer = answer;
        this.status = InquiryStatus.COMPLETE;
    }

    public void updateAnswer(String newAnswer) {
        if (this.status != InquiryStatus.COMPLETE) {
            throw new IllegalStateException("답변이 완료된 상태에서만 수정할 수 있습니다.");
        }
        this.answer = newAnswer;
    }

    public void deleteAnswer() {
        this.answer = null;
        this.status = InquiryStatus.WAITING;
    }


}
