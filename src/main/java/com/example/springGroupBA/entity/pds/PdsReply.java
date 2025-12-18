package com.example.springGroupBA.entity.pds;

import com.example.springGroupBA.dto.pds.PdsReplyDto;
import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pds_reply")
@Getter
@Setter
@ToString(exclude = {"pds", "member", "parent", "children"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class PdsReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pds_reply_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pds_id", nullable = false)
    private Pds pds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String content;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime regDate;

    @LastModifiedDate
    private LocalDateTime modDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PdsReply parent;

    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    @Builder.Default
    private List<PdsReply> children = new ArrayList<>();

    @ColumnDefault("0")
    private int reportCount;

    @ColumnDefault("false")
    private boolean isBlind;

    @ColumnDefault("false")
    private boolean isRemoved;

    public static PdsReply dtoToEntity(PdsReplyDto dto, Pds pds, Member member, PdsReply parent) {
        return PdsReply.builder()
                .id(dto.getId())
                .pds(pds)
                .member(member)
                .content(dto.getContent())
                .parent(parent)
                .isBlind(false)
                .isRemoved(false)
                .build();
    }
}