package com.example.springGroupBA.dto.pds;

import com.example.springGroupBA.entity.pds.PdsFile;
import lombok.*;

import java.text.DecimalFormat;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdsFileDto {

    private Long id;
    private Long pdsId;

    private String originalFileName;
    private String saveFileName;
    private long fileSize;
    private int downloadCount;

    public String getFileSizeStr() {
        double size = (double) fileSize;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size) + " " + units[unitIndex];
    }

    public static PdsFileDto entityToDto(PdsFile pdsFile) {
        return PdsFileDto.builder()
                .id(pdsFile.getId())
                .pdsId(pdsFile.getPds().getId())
                .originalFileName(pdsFile.getOriginalFileName())
                .saveFileName(pdsFile.getSaveFileName())
                .fileSize(pdsFile.getFileSize())
                .downloadCount(pdsFile.getDownloadCount())
                .build();
    }
}