package com.example.springGroupBA.dto.board;

import com.example.springGroupBA.entity.board.BoardFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class BoardFileDTO {

    private Long id;
    private String originalFileName;
    private String saveFileName;
    private long size;
    private String fileSizeStr;

    public BoardFileDTO(BoardFile file) {
        this.id = file.getId();
        this.originalFileName = file.getOriginalFileName();
        this.saveFileName = file.getSaveFileName();
        this.size = file.getSize();
        this.fileSizeStr = formatFileSize(file.getSize());
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        else return String.format("%.2f MB", size / (1024.0 * 1024.0));
    }
}