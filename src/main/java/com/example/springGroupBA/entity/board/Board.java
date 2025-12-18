package com.example.springGroupBA.entity.board;

import com.example.springGroupBA.entity.BaseEntity;
import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "board")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class Board extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mid", referencedColumnName = "mid")
    private Member member;

    @ColumnDefault("0")
    private int readNum;

    @ColumnDefault("0")
    private int good;

    @ColumnDefault("0")
    private int bad;

    @ColumnDefault("0")
    private int reportCount;

    @Column(length = 10)
    @ColumnDefault("'OK'")
    @Builder.Default
    private String openSw = "OK";

    @ColumnDefault("false")
    @Builder.Default
    private boolean isBlind = false;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<BoardReply> replies = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BoardReport> reports = new ArrayList<>();

    public void change(String title, String content, String openSw) {
        this.title = title;
        this.content = content;
        this.openSw = openSw;
    }

    public void changeBlind(boolean isBlind) {
        this.isBlind = isBlind;
    }
}