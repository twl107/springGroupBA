package com.example.springGroupBA.entity.notice;

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
@Table(name = "notice")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class Notice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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

    @ColumnDefault("false")
    @Builder.Default
    private boolean fixed = false;

    @Column(length = 10)
    @ColumnDefault("'OK'")
    @Builder.Default
    private String openSw = "OK";

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeFile> files = new ArrayList<>();

    public void change(String title, String content, String openSw, boolean fixed)
    {
        this.title = title;
        this.content = content;
        this.openSw = openSw;
        this.fixed = fixed;
    }

    public void increaseReadNum() {
        this.readNum++;
    }

}
