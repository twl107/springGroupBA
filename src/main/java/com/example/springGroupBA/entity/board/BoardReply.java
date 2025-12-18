package com.example.springGroupBA.entity.board;

import com.example.springGroupBA.entity.BaseEntity;
import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardReply extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mid", referencedColumnName = "mid")
    private Member member;

    @Column(length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BoardReply parent;

    @OneToMany(mappedBy = "parent", orphanRemoval = true)
    @Builder.Default
    private List<BoardReply> children = new ArrayList<>();

    private int depth;

    @ColumnDefault("false")
    @Builder.Default
    private boolean isRemoved = false;

    @ColumnDefault("0")
    @Builder.Default
    private int report = 0;

    @Builder.Default
    private boolean isBlind = false;

    @OneToMany(mappedBy = "boardReply", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardReplyReport> reports = new ArrayList<>();

    public void markAsRemoved() {
        this.isRemoved = true;
    }

}