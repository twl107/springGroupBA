package com.example.springGroupBA.service;

import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.notice.NoticeDTO;
import com.example.springGroupBA.dto.notice.NoticeFileDTO;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.notice.Notice;
import com.example.springGroupBA.entity.notice.NoticeFile;
import com.example.springGroupBA.exception.CustomRedirectException;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.notice.NoticeRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadFolder;

    private String contextPath = "/springGroupBA";

    @Transactional(readOnly = true)
    public PageResultDTO<NoticeDTO, Notice> getNoticeList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.unsorted());

        Page<Notice> result;
        if (requestDTO.getKeyword() != null && !requestDTO.getKeyword().isEmpty()) {
            result = noticeRepository.findByKeyword(requestDTO.getKeyword(), pageable);
        } else {
            result = noticeRepository.findAllDesc(pageable);
        }
        return new PageResultDTO<>(result, this::entityToDTO);
    }

    public Long setNoticeInput(NoticeDTO dto, List<MultipartFile> files, String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new CustomRedirectException("회원 정보를 찾을 수 없습니다.", "/member/memberLogin"));

        String processedContent = processContentImages(dto.getContent());

        Notice notice = Notice.builder()
                .title(dto.getTitle())
                .content(processedContent)
                .member(member)
                .fixed(dto.isFixed())
                .openSw(dto.getOpenSw() != null ? dto.getOpenSw() : "OK")
                .build();

        noticeRepository.save(notice);
        saveFiles(files, notice);
        return notice.getId();
    }

    public Map<String, Object> uploadImage(MultipartFile upload) {
        Map<String, Object> response = new HashMap<>();

        if (upload == null || upload.isEmpty()) {
            response.put("uploaded", 0);
            response.put("error", Map.of("message", "파일이 비어있습니다."));
            return response;
        }

        String uuid = UUID.randomUUID().toString().substring(0, 8);

        String originalName = upload.getOriginalFilename();
        String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.\\-]", "_") : "image.jpg";
        String fileName = uuid + "_" + safeName;

        File folder = new File(uploadFolder, "cktemp");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File saveFile = new File(folder, fileName);

        try {
            upload.transferTo(saveFile);
            response.put("uploaded", 1);
            response.put("fileName", fileName);
            response.put("url", contextPath + "/upload/cktemp/" + fileName);

        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            response.put("uploaded", 0);
            response.put("error", Map.of("message", "업로드 중 오류가 발생했습니다."));
        }
        return response;
    }

    private String processContentImages(String content) {
        if (content == null || content.isEmpty()) return "";

        Pattern pattern = Pattern.compile("/upload/cktemp/([^\"\\s?]+)");
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();

        File tempDir = new File(uploadFolder, "cktemp");
        File targetDir = new File(uploadFolder, "notice");
        if (!targetDir.exists()) targetDir.mkdirs();

        while (matcher.find()) {
            String encodedFileName = matcher.group(1);
            String fileName = null;

            try {
                fileName = URLDecoder.decode(encodedFileName, StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                fileName = encodedFileName;
            }

            File tempFile = new File(tempDir, fileName);
            File targetFile = new File(targetDir, fileName);

            if (tempFile.exists()) {
                try {
                    Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    try {
                        Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(tempFile.toPath());
                    } catch (Exception ex) {
                        log.error("이미지 파일 이동 완전 실패: " + fileName, ex);
                    }
                }
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement("/upload/notice/" + encodedFileName));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private List<String> extractContentImageFileNames(String content) {
        List<String> fileNames = new ArrayList<>();
        if (content == null || content.isEmpty()) return fileNames;

        Pattern pattern = Pattern.compile("/upload/notice/([^\"\\s?]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            try {
                String fileName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8.toString());
                fileNames.add(fileName);
            } catch (Exception e) {}
        }
        return fileNames;
    }

    public NoticeDTO getNotice(Long id, HttpSession session) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new CustomRedirectException("존재하지 않는 게시글입니다.", "/notice/noticeList"));

        Set<Long> viewedNotices = (Set<Long>) session.getAttribute("viewedNotices");
        if (viewedNotices == null) viewedNotices = new HashSet<>();

        if (!viewedNotices.contains(id)) {
            notice.increaseReadNum();
            viewedNotices.add(id);
            session.setAttribute("viewedNotices", viewedNotices);
            noticeRepository.save(notice);
        }

        NoticeDTO dto = entityToDTO(notice);

        if (dto.getContent() != null && !contextPath.isEmpty()) {
            String fixedContent = dto.getContent().replace("src=\"/upload/", "src=\"" + contextPath + "/upload/");
            dto.setContent(fixedContent);
        }

        return dto;
    }

    public void updateNotice(NoticeDTO dto, List<MultipartFile> files, List<Long> deleteFiles) {
        Notice notice = noticeRepository.findById(dto.getId())
                .orElseThrow(() -> new CustomRedirectException("게시글이 없습니다.", "/notice/noticeList"));

        List<String> oldImages = extractContentImageFileNames(notice.getContent());
        String processedContent = processContentImages(dto.getContent());
        List<String> newImages = extractContentImageFileNames(processedContent);

        oldImages.removeAll(newImages);
        for (String fileName : oldImages) {
            File file = new File(uploadFolder, "notice/" + fileName);
            if (file.exists()) file.delete();
        }

        if (deleteFiles != null && !deleteFiles.isEmpty()) {
            List<NoticeFile> existingFiles = notice.getFiles();
            List<NoticeFile> filesToRemove = new ArrayList<>();

            for (NoticeFile file : existingFiles) {
                if (deleteFiles.contains(file.getId())) {
                    File actualFile = new File(uploadFolder + "notice/", file.getSaveFileName());
                    if (actualFile.exists()) actualFile.delete();
                    filesToRemove.add(file);
                }
            }
            existingFiles.removeAll(filesToRemove);
        }

        notice.change(dto.getTitle(), processedContent, dto.getOpenSw(), dto.isFixed());
        saveFiles(files, notice);
        noticeRepository.save(notice);
    }

    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new CustomRedirectException("게시글이 없습니다.", "/notice/noticeList"));

        List<NoticeFile> files = notice.getFiles();
        if(files != null) {
            for(NoticeFile f : files) {
                try { new File(uploadFolder, "notice/" + f.getSaveFileName()).delete(); } catch(Exception e){}
            }
        }

        List<String> contentImages = extractContentImageFileNames(notice.getContent());
        for (String f : contentImages) {
            try { new File(uploadFolder, "notice/" + f).delete(); } catch(Exception e){}
        }

        noticeRepository.delete(notice);
    }

    private void saveFiles(List<MultipartFile> files, Notice notice) {
        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadFolder, "notice");
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                if(!file.isEmpty()) {
                    String uuid = UUID.randomUUID().toString().substring(0, 8);
                    String originalName = file.getOriginalFilename();
                    String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9.\\-]", "_") : "file";
                    String saveFileName = uuid + "_" + safeName;

                    try {
                        file.transferTo(new File(folder, saveFileName));
                        notice.getFiles().add(NoticeFile.builder()
                                .originalFileName(originalName)
                                .saveFileName(saveFileName)
                                .size(file.getSize())
                                .notice(notice).build());
                    } catch (IOException e){
                        throw new CustomRedirectException("파일 업로드 실패", "/notice/noticeList");
                    }
                }
            }
        }
    }

    private NoticeDTO entityToDTO(Notice notice) {
        List<NoticeFileDTO> fileDTOs = notice.getFiles().stream()
                .map(NoticeFileDTO::new).collect(Collectors.toList());

        return NoticeDTO.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .mid(notice.getMember().getMid())
                .writerName(notice.getMember().getName())
                .openSw(notice.getOpenSw())
                .readNum(notice.getReadNum())
                .fixed(notice.isFixed())
                .regDate(notice.getRegDate())
                .modDate(notice.getModDate())
                .files(fileDTOs)
                .fileCount(fileDTOs.size())
                .build();
    }
}