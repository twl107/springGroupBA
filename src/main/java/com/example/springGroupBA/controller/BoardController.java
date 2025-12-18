package com.example.springGroupBA.controller;

import com.example.springGroupBA.constant.ReportReason;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.board.BoardDTO;
import com.example.springGroupBA.dto.board.BoardReplyDTO;
import com.example.springGroupBA.service.BoardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/boardList")
    public String boardListGet(PageRequestDTO pageRequestDTO, Model model) {
        model.addAttribute("result", boardService.getBoardList(pageRequestDTO));
        return "board/boardList";
    }

    @GetMapping("/boardInput")
    public String boardInputGet() {
        return "board/boardInput";
    }

    @PostMapping("/boardInput")
    public String boardInputPost(BoardDTO dto,
                                 @RequestParam("uploadFiles") List<MultipartFile> files,
                                 @AuthenticationPrincipal UserDetails user,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/member/memberLogin";

        boardService.setBoardInput(dto, files, user.getUsername()); // 이메일 전달
        redirectAttributes.addFlashAttribute("message", "게시글이 등록되었습니다.");
        return "redirect:/board/boardList";
    }

    @PostMapping("/imageUpload")
    @ResponseBody
    public Map<String, Object> imageUpload(@RequestParam("upload") MultipartFile upload) {
        return boardService.uploadImage(upload);
    }

    @GetMapping("/boardContent")
    public String boardContentGet(@RequestParam("id") Long id,
                                  @ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                                  @AuthenticationPrincipal UserDetails user,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {

        String email = (user != null) ? user.getUsername() : "guest";
        boolean isAdmin = request.isUserInRole("ROLE_ADMIN");

        BoardDTO dto = boardService.getBoard(id, request.getSession(), email);

        if (dto.getIsBlind() && !isAdmin) {
            redirectAttributes.addFlashAttribute("message", "블라인드된 게시글입니다.");
            return "redirect:/board/boardList?page=" + pageRequestDTO.getPage();
        }

        if ("NO".equals(dto.getOpenSw())) {
            if (!email.equals(dto.getEmail()) && !isAdmin) {
                redirectAttributes.addFlashAttribute("message", "비공개 게시글입니다.");
                return "redirect:/board/boardList?page=" + pageRequestDTO.getPage();
            }
        }

        model.addAttribute("dto", dto);
        model.addAttribute("replyList", boardService.getReplyList(id));

        System.out.println("dto ===============> " + dto);
        return "board/boardContent";
    }

    @GetMapping("/boardUpdate")
    public String boardUpdateGet(@RequestParam Long id,
                                 @ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                                 @AuthenticationPrincipal UserDetails user,
                                 HttpServletRequest request,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/member/memberLogin";

        BoardDTO dto = boardService.getBoard(id, request.getSession(), user.getUsername());

        if (!dto.getEmail().equals(user.getUsername()) && !request.isUserInRole("ROLE_ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "수정 권한이 없습니다.");
            return "redirect:/board/boardContent?id=" + id;
        }

        model.addAttribute("dto", dto);
        return "board/boardUpdate";
    }

    @PostMapping("/boardUpdate")
    public String boardUpdatePost(BoardDTO dto,
                                  @RequestParam(value = "uploadFiles", required = false) List<MultipartFile> uploadFiles,
                                  @RequestParam(value = "deleteFiles", required = false) List<Long> deleteFiles,
                                  @ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                                  RedirectAttributes redirectAttributes) {

        boardService.modify(dto, uploadFiles, deleteFiles);

        redirectAttributes.addFlashAttribute("message", "수정되었습니다.");
        redirectAttributes.addAttribute("id", dto.getId());
        redirectAttributes.addAttribute("page", pageRequestDTO.getPage());
        redirectAttributes.addAttribute("type", pageRequestDTO.getType());
        redirectAttributes.addAttribute("keyword", pageRequestDTO.getKeyword());

        return "redirect:/board/boardContent";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        boardService.deleteBoard(id);
        redirectAttributes.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/board/boardList";
    }

    @GetMapping("/vote/{id}")
    public String vote(@PathVariable Long id, @RequestParam String type,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {

        boolean success = boardService.setGoodBad(id, type, request.getSession());
        if (success) redirectAttributes.addFlashAttribute("message", "반영되었습니다.");
        else redirectAttributes.addFlashAttribute("message", "이미 참여하셨습니다.");

        return "redirect:/board/boardContent?id=" + id;
    }

    @PostMapping("/report")
    public String reportBoard(
            @RequestParam Long boardId,
            @RequestParam ReportReason reason,
            @RequestParam(required = false) String customReason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {

        String mid = userDetails.getUsername();
        boardService.reportBoard(boardId, mid, reason, customReason);

        redirectAttributes.addFlashAttribute("message", "신고가 정상적으로 접수되었습니다.");
        return "redirect:/board/boardContent?id=" + boardId;
    }

    @PostMapping("/reply/register")
    public String replyRegister(BoardReplyDTO dto,
                                @AuthenticationPrincipal UserDetails user,
                                RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/member/memberLogin";

        dto.setMid(user.getUsername());
        boardService.registerReply(dto);
        redirectAttributes.addFlashAttribute("message", "댓글이 등록되었습니다.");
        return "redirect:/board/boardContent?id=" + dto.getBoardId();
    }

    @PostMapping("/reply/delete")
    public String replyDelete(Long replyId, Long boardId, RedirectAttributes rttr) {
        boardService.deleteReply(replyId);
        rttr.addFlashAttribute("message", "댓글이 삭제되었습니다.");
        return "redirect:/board/boardContent?id=" + boardId;
    }

    @PostMapping("/reply/update")
    public String replyUpdate(Long id, Long boardId, String content, RedirectAttributes rttr) {
        boardService.updateReply(id, content);
        rttr.addFlashAttribute("message", "댓글이 수정되었습니다.");
        return "redirect:/board/boardContent?id=" + boardId;
    }

    @PostMapping("/reply/report")
    public String reportReply(
            @RequestParam Long replyId,
            @RequestParam Long boardId,
            @RequestParam ReportReason reason,
            @RequestParam(required = false) String customReason,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes rttr) {

        String email = userDetails.getUsername();

        boardService.reportReply(replyId, email, reason, customReason);

        rttr.addFlashAttribute("message", "댓글 신고가 접수되었습니다.");
        return "redirect:/board/boardContent?id=" + boardId;
    }
}