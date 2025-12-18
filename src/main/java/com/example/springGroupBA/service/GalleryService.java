package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.gallery.Gallery;
import com.example.springGroupBA.dto.gallery.GalleryDTO;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.gallery.GalleryRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final MemberRepository memberRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadFolder;

    @Transactional(readOnly = true)
    public Page<GalleryDTO> getList(Pageable pageable) {
        return galleryRepository.findAll(pageable).map(GalleryDTO::entityToDto);
    }

    public void register(GalleryDTO dto, MultipartFile file, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        String saveFileName = saveFile(file);

        Gallery gallery = Gallery.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .fileName(saveFileName)
                .member(member)
                .build();

        galleryRepository.save(gallery);
    }

    public void modify(GalleryDTO dto, MultipartFile file, String email, boolean isAdmin) {
        Gallery gallery = galleryRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!gallery.getMember().getEmail().equals(email) && !isAdmin) {
            throw new SecurityException("수정 권한이 없습니다.");
        }

        String newFileName = gallery.getFileName();
        if (file != null && !file.isEmpty()) {
            deleteFile(gallery.getFileName());
            newFileName = saveFile(file);
        }
        gallery.change(dto.getTitle(), dto.getContent(), newFileName);
    }

    public void delete(Long id, String email, boolean isAdmin) {
        Gallery gallery = galleryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!gallery.getMember().getEmail().equals(email) && !isAdmin) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }
        deleteFile(gallery.getFileName());

        galleryRepository.delete(gallery);
    }


    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String originalFilename = file.getOriginalFilename();
        String saveFileName = uuid + "_" + originalFilename;

        try {
            File folder = new File(uploadFolder + "gallery");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File destination = new File(folder, saveFileName);
            file.transferTo(destination);
            return saveFileName;
        }
        catch (Exception e) {
            log.error("파일 저장 실패", e);
            throw new RuntimeException("파일 저장 중 오류 발생");
        }
    }

    private void deleteFile(String fileName) {
        if (fileName != null) {
            File file = new File(uploadFolder + "gallery/" + fileName);
            if (file.exists()) {
                if (file.delete()) {
                    log.info("파일 삭제 성공: {}", file.getAbsolutePath());
                }
                else {
                    log.info("파일 삭제 실패: {}", file.getAbsolutePath());
                }
            }
        }
    }
}