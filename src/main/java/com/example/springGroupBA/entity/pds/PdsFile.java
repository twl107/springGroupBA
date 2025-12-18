package com.example.springGroupBA.entity.pds;

import com.example.springGroupBA.dto.pds.PdsFileDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "pds_file")
@Getter
@Setter
@ToString(exclude = "pds")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@EntityListeners(AuditingEntityListener.class)
public class PdsFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pds_file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pds_id", nullable = false)
    private Pds pds;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String saveFileName;

    private long fileSize;

    @ColumnDefault("0")
    private int downloadCount;

    public void setPds(Pds pds) {
        this.pds = pds;
        if (pds != null && !pds.getPdsFiles().contains(this)) {
            pds.getPdsFiles().add(this);
        }
    }

    public static PdsFile dtoToEntity(PdsFileDto dto, Pds pds) {
        return PdsFile.builder()
                .id(dto.getId())
                .pds(pds) // 연관된 게시글 객체 주입
                .originalFileName(dto.getOriginalFileName())
                .saveFileName(dto.getSaveFileName())
                .fileSize(dto.getFileSize())
                .downloadCount(dto.getDownloadCount())
                .build();
    }

}
