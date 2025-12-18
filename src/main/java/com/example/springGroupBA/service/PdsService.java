package com.example.springGroupBA.service;

import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.pds.PdsDto;
import com.example.springGroupBA.dto.pds.PdsReplyDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.pds.Pds;
import com.example.springGroupBA.entity.pds.PdsFile;
import com.example.springGroupBA.entity.pds.PdsReply;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.pds.PdsFileRepository;
import com.example.springGroupBA.repository.pds.PdsReplyRepository;
import com.example.springGroupBA.repository.pds.PdsRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PdsService {

    private final PdsRepository pdsRepository;
    private final PdsFileRepository pdsFileRepository;
    private final MemberRepository memberRepository;
    private final PdsReplyRepository pdsReplyRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadFolder;

    public PageResultDTO<PdsDto, Pds> getList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.by("id").descending());
        Page<Pds> result = pdsRepository.searchPage(requestDTO.getType(), requestDTO.getKeyword(), pageable);
        Function<Pds, PdsDto> fn = (entity -> PdsDto.entityToDto(entity));
        return new PageResultDTO<>(result, fn);
    }

    public Long setPdsInput(PdsDto pdsDto, List<MultipartFile> files) {
        Member member = memberRepository.findByEmail(pdsDto.getMid())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다."));

        Pds pds = Pds.dtoToEntity(pdsDto, member);

        if(files != null && !files.isEmpty()){
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String uuid = UUID.randomUUID().toString().substring(0,8);
                String saveName = uuid + "_" + file.getOriginalFilename();
                try {
                    File saveFile = new File(uploadFolder + "pds/" + saveName);

                    if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
                    file.transferTo(saveFile);
                    PdsFile pdsFile = PdsFile.builder()
                            .originalFileName(file.getOriginalFilename())
                            .saveFileName(saveName)
                            .fileSize(file.getSize())
                            .pds(pds)
                            .build();
                    pds.addFile(pdsFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        pdsRepository.save(pds);
        return pds.getId();
    }

    public PdsDto getPdsContent(Long id, HttpSession session, String email) {
        Pds pds = pdsRepository.findById(id).orElseThrow(() -> new RuntimeException("게시글 없음"));

        if (!pds.getMember().getMid().equals(email)) {
            String sessionKey = "read_pds_" + id;
            if (session.getAttribute(sessionKey) == null) {
                pdsRepository.updateReadCount(id);
                session.setAttribute(sessionKey, true);
            }
        }

        return PdsDto.entityToDto(pds);
    }

    public void setPdsUpdate(PdsDto pdsDto, List<MultipartFile> files, List<Long> deleteFileIds) {
        Pds pds = pdsRepository.findById(pdsDto.getId()).orElseThrow();
        pds.setTitle(pdsDto.getTitle());
        pds.setContent(pdsDto.getContent());
        pds.setOpenSw(pdsDto.getOpenSw());

        boolean dataChanged = false;

        if (deleteFileIds != null) {
            for (Long fileId : deleteFileIds) {
                PdsFile pdsFile = pdsFileRepository.findById(fileId).orElse(null);
                if (pdsFile != null) {
                    File deleteFile = new File(uploadFolder + "pds/" + pdsFile.getSaveFileName());
                    if (deleteFile.exists()) deleteFile.delete();
                    pds.getPdsFiles().remove(pdsFile);
                    pdsFileRepository.delete(pdsFile);
                    dataChanged = true;
                }
            }
        }

        if(files != null && !files.isEmpty()){
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String uuid = UUID.randomUUID().toString().substring(0,8);
                String saveName = uuid + "_" + file.getOriginalFilename();
                try {
                    File saveFile = new File(uploadFolder + "pds/" + saveName);
                    if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
                    file.transferTo(saveFile);
                    PdsFile pdsFile = PdsFile.builder()
                            .originalFileName(file.getOriginalFilename())
                            .saveFileName(saveName)
                            .fileSize(file.getSize())
                            .pds(pds)
                            .build();
                    pds.addFile(pdsFile);
                    dataChanged = true;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (dataChanged) pds.setModDate(LocalDateTime.now());
    }

    public void delete(Long id) {
        Pds pds = pdsRepository.findById(id).orElseThrow();
        List<PdsFile> files = pds.getPdsFiles();
        if(files != null) {
            for (PdsFile file : files) {
                File deleteFile = new File(uploadFolder + "pds/" + file.getSaveFileName());
                if(deleteFile.exists()) deleteFile.delete();
            }
        }
        pdsRepository.deleteById(id);
    }

    public ResponseEntity<Resource> downloadFile(Long fileId) {
        PdsFile fileEntity = pdsFileRepository.findById(fileId).orElseThrow();
        fileEntity.setDownloadCount(fileEntity.getDownloadCount() + 1);
        pdsFileRepository.save(fileEntity);

        Path path = Paths.get(uploadFolder + "pds/" + fileEntity.getSaveFileName());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) throw new RuntimeException("파일 없음");

            String encodedFileName = UriUtils.encode(fileEntity.getOriginalFileName(), StandardCharsets.UTF_8);
            String contentDisposition = "attachment; filename=\"" + encodedFileName + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean setGoodBad(Long id, String type, HttpSession session, String mid) {
        String sessionKey = "vote_pds_" + id;
        if (session.getAttribute(sessionKey) != null) return false;

        if ("good".equals(type)) pdsRepository.updateGoodCount(id);
        else pdsRepository.updateBadCount(id);

        session.setAttribute(sessionKey, type);
        return true;
    }

    public List<PdsReplyDto> getReplyList(Long pdsId) {
        Pds pds = pdsRepository.findById(pdsId).orElseThrow();
        List<PdsReply> allReplies = pdsReplyRepository.findByPdsOrderByRegDateAsc(pds);
        List<PdsReplyDto> result = new ArrayList<>();
        for (PdsReply reply : allReplies) {
            if (reply.getParent() == null) addReplyAndChildren(reply, result, 0);
        }
        return result;
    }

    private void addReplyAndChildren(PdsReply parent, List<PdsReplyDto> result, int depth) {
        PdsReplyDto dto = PdsReplyDto.entityToDto(parent);
        dto.setDepth(depth);
        result.add(dto);
        if (parent.getChildren() != null) {
            for (PdsReply child : parent.getChildren()) addReplyAndChildren(child, result, depth + 1);
        }
    }

    public void saveReply(PdsReplyDto dto) {
        Pds pds = pdsRepository.findById(dto.getPdsId()).orElseThrow();
        Member member = memberRepository.findByEmail(dto.getMid()).orElseThrow();
        PdsReply parent = null;
        if (dto.getParentId() != null) {
            parent = pdsReplyRepository.findById(dto.getParentId()).orElse(null);
        }
        pdsReplyRepository.save(PdsReply.dtoToEntity(dto, pds, member, parent));
    }

    public void updateReply(PdsReplyDto dto) {
        PdsReply reply = pdsReplyRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("댓글이 존재하지 않습니다."));

        reply.setContent(dto.getContent().trim());
        reply.setModDate(LocalDateTime.now());
    }

    public void deleteReply(Long replyId) {
        PdsReply reply = pdsReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        int childCount = pdsReplyRepository.countByParentId(replyId);

        if (childCount > 0) {
            reply.setRemoved(true);
        } else {
            PdsReply parent = reply.getParent();

            pdsReplyRepository.delete(reply);

            while (parent != null && parent.isRemoved()) {
                int parentChildCount = pdsReplyRepository.countByParentId(parent.getId());

                if (parentChildCount == 0) {
                    PdsReply nextParent = parent.getParent();
                    pdsReplyRepository.delete(parent);
                    parent = nextParent;
                } else {
                    break;
                }
            }
        }
    }
}