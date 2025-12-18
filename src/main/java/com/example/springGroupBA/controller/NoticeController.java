package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.notice.NoticeDTO;
import com.example.springGroupBA.exception.CustomRedirectException;
import com.example.springGroupBA.service.NoticeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/notice")
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/noticeList")
    public String noticeListGet(@ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO, Model model) {
        model.addAttribute("result", noticeService.getNoticeList(pageRequestDTO));
        return "notice/noticeList";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/noticeInput")
    public String noticeInputGet() {
        return "notice/noticeInput";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/noticeInput")
    public String noticeInputPost(NoticeDTO dto,
                                  @RequestParam(value = "uploadFiles", required = false)List<MultipartFile> files,
                                  @AuthenticationPrincipal UserDetails user,
                                  RedirectAttributes redirectAttributes) {

        noticeService.setNoticeInput(dto, files, user.getUsername());
        redirectAttributes.addFlashAttribute("message", "공지사항이 등록되었습니다.");
        return "redirect:/notice/noticeList";
    }

    @GetMapping("/noticeContent")
    public String noticeContentGet(@RequestParam("id") Long id,
                                   @ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                                   HttpServletRequest request,
                                   Model model) {

        NoticeDTO dto = noticeService.getNotice(id, request.getSession());

        boolean isAdmin = request.isUserInRole("ADMIN");
        if ("NO".equals(dto.getOpenSw()) && !isAdmin) {
            String redirectUrl = (request.getUserPrincipal() == null) ? "/member/memberLogin" : "/notice/noticeList";
            throw new CustomRedirectException("비공개 게시글입니다.", redirectUrl);
        }
        model.addAttribute("dto", dto);
        return "notice/noticeContent";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/noticeUpdate")
    public String noticeUpdateGet(@RequestParam Long id,
                                  @ModelAttribute("pageRequestDTO") PageRequestDTO pageRequestDTO,
                                  HttpServletRequest request,
                                  Model model) {

        NoticeDTO dto = noticeService.getNotice(id, request.getSession());
        model.addAttribute("dto", dto);
        return "notice/noticeUpdate";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/noticeUpdate")
    public String noticeUpdatePost(NoticeDTO dto, Long id,
                                   @RequestParam(value = "uploadFiles", required = false) List<MultipartFile> files,
                                   @RequestParam(value = "deleteFiles", required = false) List<Long> deleteFiles,
                                   PageRequestDTO pageRequestDTO,
                                   RedirectAttributes redirectAttributes) {

        noticeService.updateNotice(dto, files, deleteFiles);

        redirectAttributes.addFlashAttribute("message", "수정되었습니다.");
        redirectAttributes.addFlashAttribute("id", dto.getId());
        redirectAttributes.addAttribute("page", pageRequestDTO.getPage());

        return "redirect:/notice/noticeContent?id=" + id;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/noticeDelete")
    public String noticeDelete(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        noticeService.deleteNotice(id);
        redirectAttributes.addFlashAttribute("message", "삭제되었습니다.");
        return "redirect:/notice/noticeList";
    }

    @ResponseBody
    @PostMapping("/imageUpload")
    public Map<String, Object> imageUpload(@RequestParam("upload") MultipartFile upload) {
        return noticeService.uploadImage(upload);
    }
}