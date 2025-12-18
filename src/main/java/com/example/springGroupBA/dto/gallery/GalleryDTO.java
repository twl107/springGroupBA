package com.example.springGroupBA.dto.gallery;

import com.example.springGroupBA.entity.gallery.Gallery;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GalleryDTO {

    private Long id;
    private String title;
    private String content;
    private String fileName;
    private String writerName;
    private String writerEmail;
    private LocalDateTime regDate;

    public static GalleryDTO entityToDto(Gallery gallery) {
        return GalleryDTO.builder()
                .id(gallery.getId())
                .title(gallery.getTitle())
                .content(gallery.getContent())
                .fileName(gallery.getFileName())
                .writerName(gallery.getMember().getName())
                .writerEmail(gallery.getMember().getEmail())
                .regDate(gallery.getRegDate())
                .build();
    }
}
