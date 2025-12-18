package com.example.springGroupBA.entity.gallery;

import com.example.springGroupBA.entity.BaseEntity;
import com.example.springGroupBA.entity.member.Member;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "member")
@Table(name = "gallery")
public class Gallery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String content;

    private String fileName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mid")
    private Member member;

    public void change(String title, String content, String newFileName) {
        this.title = title;
        this.content = content;
        this.fileName = newFileName;
    }
}
