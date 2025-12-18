package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.ReportReason;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.PageResultDTO;
import com.example.springGroupBA.dto.board.BoardDTO;
import com.example.springGroupBA.dto.board.BoardReplyDTO;
import com.example.springGroupBA.entity.board.*;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.board.*;
import com.example.springGroupBA.repository.member.MemberRepository;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardReportRepository boardReportRepository;
    private final BoardReplyReportRepository boardReplyReportRepository;
    private final BoardReplyRepository boardReplyRepository;
    private final BoardFileRepository boardFileRepository;
    private final MemberRepository memberRepository;

    @Value("${org.zerock.upload.path}")
    private String uploadFolder;

    private String contextPath = "/springGroupBA";

    @Transactional(readOnly = true)
    public PageResultDTO<BoardDTO, Board> getBoardList(PageRequestDTO requestDTO) {
        Pageable pageable = requestDTO.getPageable(Sort.by("id").descending());
        Page<Board> result = boardRepository.searchPage(requestDTO.getType(), requestDTO.getKeyword(), pageable);
        Function<Board, BoardDTO> fn = (BoardDTO::entityToDto);
        return new PageResultDTO<>(result, fn);
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
        if (!folder.exists()) folder.mkdirs();

        File saveFile = new File(folder, fileName);

        try {
            upload.transferTo(saveFile);
            response.put("uploaded", 1);
            response.put("fileName", fileName);
            response.put("url", contextPath + "/upload/cktemp/" + fileName);
        }
        catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            response.put("uploaded", 0);
            response.put("error", Map.of("message", "업로드 중 오류가 발생했습니다."));
        }
        return response;
    }

    public void setBoardInput(BoardDTO dto, List<MultipartFile> files, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보 없음"));

        String processedContent = processContentImages(dto.getContent());
        String openSwVal = "NO".equals(dto.getOpenSw()) ? "NO" : "OK";

        Board board = Board.builder()
                .title(dto.getTitle())
                .content(processedContent)
                .member(member)
                .openSw(openSwVal)
                .isBlind(false)
                .reportCount(0)
                .build();

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String original = file.getOriginalFilename();
                String stored = UUID.randomUUID().toString().substring(0, 8) + "_" + original;
                try {
                    File saveFile = new File(uploadFolder + "board/" + stored);
                    if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
                    file.transferTo(saveFile);

                    board.getFiles().add(BoardFile.builder()
                            .originalFileName(original)
                            .saveFileName(stored)
                            .size(file.getSize())
                            .board(board)
                            .build());
                } catch (Exception e) {
                    log.error("파일 업로드 실패", e);
                }
            }
        }
        boardRepository.save(board);
    }

    public BoardDTO getBoard(Long id, HttpSession session, String userEmail) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        String sessionKey = "read_board_" + id;

        if (session.getAttribute(sessionKey) == null) {
            board.setReadNum(board.getReadNum() + 1);
            session.setAttribute(sessionKey, true);
        }

        BoardDTO dto = BoardDTO.entityToDto(board);

        if (dto.getContent() != null && !contextPath.isEmpty()) {
            String fixedContent = dto.getContent().replace("src=\"/upload/", "src=\"" + contextPath + "/upload/");
            dto.setContent(fixedContent);
        }

        return dto;
    }

    public void modify(BoardDTO boardDTO, List<MultipartFile> files, List<Long> deleteFileIds) {
        Board board = boardRepository.findById(boardDTO.getId())
                .orElseThrow(() -> new IllegalArgumentException("게시글이 없습니다."));

        List<String> oldImages = extractContentImageFileNames(board.getContent());
        String processedContent = processContentImages(boardDTO.getContent());
        List<String> newImages = extractContentImageFileNames(processedContent);

        oldImages.removeAll(newImages);
        for (String fileName : oldImages) {
            File file = new File(uploadFolder + "board/" + fileName);
            if (file.exists()) file.delete();
        }

        board.change(boardDTO.getTitle(), processedContent, boardDTO.getOpenSw());

        if(deleteFileIds != null) {
            for (Long fileId : deleteFileIds) {
                BoardFile boardFile = boardFileRepository.findById(fileId).orElse(null);
                if (boardFile != null) {
                    File deleteFile = new File(uploadFolder + "board/" + boardFile.getSaveFileName());
                    if (deleteFile.exists()) deleteFile.delete();
                    board.getFiles().remove(boardFile);
                    boardFileRepository.delete(boardFile);
                }
            }
        }

        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String saveName = UUID.randomUUID().toString().substring(0, 8) + "_" + file.getOriginalFilename();
                try {
                    File saveFile = new File(uploadFolder + "board/" + saveName);
                    if (!saveFile.getParentFile().exists()) saveFile.getParentFile().mkdirs();
                    file.transferTo(saveFile);

                    board.getFiles().add(BoardFile.builder()
                            .originalFileName(file.getOriginalFilename())
                            .saveFileName(saveName)
                            .size(file.getSize())
                            .board(board)
                            .build());
                } catch (Exception e) {
                    log.error("파일 추가 실패", e);
                }
            }
        }
    }

    @Transactional
    public void deleteBoard(Long id) {
        Board board = boardRepository.findById(id).orElseThrow();

        if(board.getFiles() != null) {
            for(BoardFile file : board.getFiles()) {
                File deleteFile = new File(uploadFolder + "board/" + file.getSaveFileName());
                if(deleteFile.exists()) deleteFile.delete();
            }
        }

        List<String> contentImages = extractContentImageFileNames(board.getContent());
        for (String fileName : contentImages) {
            try {
                new File(uploadFolder + "board/" + fileName).delete();
            }
            catch (Exception e) {}
        }
        boardRepository.delete(board);
    }

    public boolean setGoodBad(Long id, String type, HttpSession session) {
        String sessionKey = "vote_board_" + id;

        if (session.getAttribute(sessionKey) != null) return false;

        if ("good".equals(type)) boardRepository.updateGoodCount(id);
        else boardRepository.updateBadCount(id);

        session.setAttribute(sessionKey, type);
        return true;
    }

    public void reportBoard(Long boardId, String mid, ReportReason reason, String customReason) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다. id=" + boardId));

        Member member = memberRepository.findByEmail(mid)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. mid=" + mid));

        BoardReport report = BoardReport.builder()
                .board(board)
                .member(member)
                .reason(reason)
                .customReason(customReason)
                .build();
        boardReportRepository.save(report);

        board.setReportCount(board.getReportCount() + 1);
        if (board.getReportCount() >= 3) {
            board.changeBlind(true);
        }
    }

    public void registerReply(BoardReplyDTO dto) {
        Member member = memberRepository.findByEmail(dto.getMid()).orElseThrow();
        Board board = boardRepository.findById(dto.getBoardId()).orElseThrow();

        BoardReply parent = null;
        int depth = 0;

        if (dto.getParentId() != null) {
            parent = boardReplyRepository.findById(dto.getParentId()).orElseThrow();
            depth = parent.getDepth() + 1;
        }

        BoardReply reply = BoardReply.builder()
                .board(board)
                .member(member)
                .content(dto.getContent().trim())
                .parent(parent)
                .depth(depth)
                .report(0)
                .isBlind(false)
                .isRemoved(false)
                .build();

        boardReplyRepository.save(reply);
    }

    @Transactional(readOnly = true)
    public List<BoardReplyDTO> getReplyList(Long boardId) {

        List<BoardReply> allReplies = boardReplyRepository.findAllByBoardIdOrderByRegDateAsc(boardId);

        List<BoardReplyDTO> result = new ArrayList<>();

        for (BoardReply reply : allReplies) {
            if (reply.getParent() == null) {
                addReplyAndChildren(reply, result);
            }
        }
        return result;
    }

    private void addReplyAndChildren(BoardReply parent, List<BoardReplyDTO> result) {
        result.add(BoardReplyDTO.entityToDto(parent));

        if (parent.getChildren() != null && !parent.getChildren().isEmpty()) {
            for (BoardReply child : parent.getChildren()) {
                addReplyAndChildren(child, result);
            }
        }
    }

    public void updateReply(Long replyId, String content) {
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow();
        reply.setContent(content.trim());
    }

    public void deleteReply(Long replyId) {
        BoardReply reply = boardReplyRepository.findById(replyId).orElseThrow();

        int childCount = boardReplyRepository.countByParentId(replyId);

        if (childCount > 0) {
            reply.markAsRemoved();
        }
        else {
            BoardReply parent = reply.getParent();
            boardReplyRepository.delete(reply);

            while (parent != null && parent.isRemoved()) {
                if (boardReplyRepository.countByParentId(parent.getId()) == 0) {
                    BoardReply nextParent = parent.getParent();
                    boardReplyRepository.delete(parent);
                    parent = nextParent;
                }
                else break;
            }
        }
    }

    @Transactional
    public void reportReply(Long replyId, String email, ReportReason reason, String customReason) {
        BoardReply reply = boardReplyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다. email=" + email));

        BoardReplyReport report = BoardReplyReport.builder()
                .boardReply(reply)
                .member(member)
                .reason(reason)
                .customReason(customReason)
                .build();
        boardReplyReportRepository.save(report);

        reply.setReport(reply.getReport() + 1);
        if (reply.getReport() >= 3) {
            reply.setBlind(true);
        }
    }

    private String processContentImages(String content) {
        if (content == null || content.isEmpty()) return "";

        Pattern pattern = Pattern.compile("/upload/cktemp/([^\"\\s?]+)");
        Matcher matcher = pattern.matcher(content);

        StringBuffer sb = new StringBuffer();

        File tempDir = new File(uploadFolder, "cktemp");
        File targetDir = new File(uploadFolder, "board");
        if (!targetDir.exists()) targetDir.mkdirs();

        while (matcher.find()) {
            String encodedFileName = matcher.group(1);
            String fileName;
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
                        log.error("이미지 파일 이동 실패: " + fileName, ex);
                    }
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement("/upload/board/" + encodedFileName));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private List<String> extractContentImageFileNames(String content) {
        List<String> fileNames = new ArrayList<>();
        if (content == null || content.isEmpty()) return fileNames;

        Pattern pattern = Pattern.compile("/upload/board/([^\"\\s?]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            try {
                String fileName = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8.toString());
                fileNames.add(fileName);
            } catch (Exception e) {}
        }
        return fileNames;
    }
}