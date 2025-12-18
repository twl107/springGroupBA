package com.example.springGroupBA.entity.pds;

import com.example.springGroupBA.dto.pds.PdsDto;
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
@Table(name = "pds")
@Getter
@Setter
@ToString(exclude = {"member", "pdsReplies", "pdsFiles"})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class Pds {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pds_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mid", referencedColumnName = "mid")
    private Member member;

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    private String content;

    @ColumnDefault("'OK'")
    private String openSw;

    @ColumnDefault("0")
    private int readNum;

    @ColumnDefault("0")
    private int downNum;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime regDate;

    @LastModifiedDate
    private LocalDateTime modDate;

    @ColumnDefault("0")
    private int good;

    @ColumnDefault("0")
    private int bad;

    @ColumnDefault("0")
    @Builder.Default
    private int reportCount = 0;

    @ColumnDefault("false")
    @Builder.Default
    private boolean isBlind = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "pds", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PdsReply> pdsReplies = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "pds", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PdsFile> pdsFiles = new ArrayList<>();

    @Transient
    private long hourDiff;

    @Transient
    private long dateDiff;

    @Transient
    private long replyCnt;

    public void addFile(PdsFile file) {
        this.pdsFiles.add(file);
        file.setPds(this);
    }

    public static Pds dtoToEntity(PdsDto dto, Member member) {
        return Pds.builder()
                .id(dto.getId())
                .member(member)
                .title(dto.getTitle())
                .content(dto.getContent())
                .openSw(dto.getOpenSw() != null ? dto.getOpenSw() : "OK")
                .readNum(dto.getReadNum())
                .downNum(dto.getDownNum())
                .good(dto.getGood())
                .bad(dto.getBad())
                .build();
    }
}
