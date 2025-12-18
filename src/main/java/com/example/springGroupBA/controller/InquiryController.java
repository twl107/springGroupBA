package com.example.springGroupBA.controller;

import com.example.springGroupBA.constant.InquiryStatus;
import com.example.springGroupBA.dto.Inquiry.AdminAnswerDTO;
import com.example.springGroupBA.dto.Inquiry.InquiryDTO;
import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.service.InquiryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/inquiryList")
    public String inquiryListGet(PageRequestDTO pageRequestDTO,
                                @RequestParam(value = "status", required = false) InquiryStatus status,
                                Model model, Principal principal) {

        String email = principal.getName();
        model.addAttribute("result", inquiryService.getInquiryList(email, pageRequestDTO, status));
        model.addAttribute("status", status);
        return "inquiry/inquiryList";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/adminList")
    public String adminInquiryList(PageRequestDTO pageRequestDTO,
                                    @RequestParam(value = "status", required = false) InquiryStatus status,
                                    Model model) {

        model.addAttribute("result", inquiryService.getAllInquiryList(pageRequestDTO, status));
        model.addAttribute("status", status);
        return "inquiry/adminList";
    }

    @GetMapping("/inquiryInput")
    public String inquiryInputGet() {
        return "inquiry/inquiryInput";
    }

    @PostMapping("/inquiryInput")
    public String inquiryInputPost(InquiryDTO dto, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.setInquiryInput(dto, principal.getName());
            redirectAttributes.addFlashAttribute("message", "문의가 등록되었습니다.");
        }
        catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 중 오류가 발생했습니다." + e.getMessage());
        }
        return "redirect:/inquiry/inquiryList";
    }

    @GetMapping("/inquiryContent")
    public String inquiryContentGet(Long id, PageRequestDTO pageRequestDTO,
                                    @RequestParam(value = "status", required = false) InquiryStatus status,
                                    @RequestParam(value = "link", required = false) String link,
                                    Model model) {

        InquiryDTO dto = inquiryService.getInquiry(id);
        model.addAttribute("dto", dto);
        model.addAttribute("pageRequestDTO", pageRequestDTO);
        model.addAttribute("status", status);
        model.addAttribute("link", link);
        return "inquiry/inquiryContent";
    }

    @GetMapping("/inquiryUpdate")
    public String inquiryUpdateGet(Long id, Model model, Principal principal) {
        InquiryDTO dto = inquiryService.getInquiry(id);
        model.addAttribute("dto", dto);
        return "inquiry/inquiryUpdate";
    }

    @PostMapping("/inquiryUpdate")
    public String inquiryUpdatePost(InquiryDTO dto,PageRequestDTO pageRequestDTO,
                                    @RequestParam(value = "status", required = false) String statusStr,
                                    Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.setInquiryUpdate(dto, principal.getName());
            redirectAttributes.addFlashAttribute("message", "수정되었습니다.");
        }
        catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return  "redirect:/inquiry/inquiryList";
        }
        return "redirect:/inquiry/inquiryContent?id=" + dto.getId() +
                "&page=" + pageRequestDTO.getPage() +
                "&type=" + (pageRequestDTO.getType() != null ? pageRequestDTO.getType() : "") +
                "&keyword=" + (pageRequestDTO.getKeyword() != null ? pageRequestDTO.getKeyword() : "") +
                "&status=" + (statusStr != null ? statusStr : "");
    }

    @PostMapping("/inquiryDelete")
    public String inquiryDeletePost(Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.setInquiryDelete(id, principal.getName());
            redirectAttributes.addFlashAttribute("message", "삭제되었습니다.");
        }
        catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inquiry/inquiryList";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/answer")
    public String answerInquiry(AdminAnswerDTO dto, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.answerInquiry(dto, principal.getName());
            redirectAttributes.addFlashAttribute("message", "답변이 성공적으로 등록되었습니다.");
        }
        catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inquiry/inquiryContent?id=" + dto.getId();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/modifyAnswer")
    public String modifyAnswer(AdminAnswerDTO dto, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.modifyAdminAnswer(dto, principal.getName());
            redirectAttributes.addFlashAttribute("message", "답변이 수정되었습니다.");
        }
        catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inquiry/inquiryContent?id=" + dto.getId();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/deleteAnswer")
    public String deleteAnswer(Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            inquiryService.deleteAdminAnswer(id, principal.getName());
            redirectAttributes.addFlashAttribute("message", "답변이 삭제되었습니다.");
        }
        catch (EntityNotFoundException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inquiry/inquiryContent?id=" + id;
    }



}
