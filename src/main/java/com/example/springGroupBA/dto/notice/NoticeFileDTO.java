package com.example.springGroupBA.dto.notice;

import com.example.springGroupBA.entity.notice.NoticeFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeFileDTO {

    private Long id;
    private String originalFileName;
    private String saveFileName;
    private long size;
    private String fileSizeStr;

    public NoticeFileDTO(NoticeFile file) {
        this.id = file.getId();
        this.originalFileName = file.getOriginalFileName();
        this.saveFileName = file.getSaveFileName();
        this.size = file.getSize();
        this.fileSizeStr = formatFilesSize(file.getSize());
    }

    private String formatFilesSize(long size) {
        if (size <= 1024) return " B";
        else if (size <= 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        else return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }

}
