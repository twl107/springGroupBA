package com.example.springGroupBA.controller;

import com.example.springGroupBA.dto.PageRequestDTO;
import com.example.springGroupBA.dto.pds.PdsDto;
import com.example.springGroupBA.dto.pds.PdsReplyDto;
import com.example.springGroupBA.service.PdsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pds")
@Slf4j
public class PdsController {

    private final PdsService pdsService;

    @GetMapping("/pdsList")
    public String pdsListGet(PageRequestDTO pageRequestDTO, Model model, Principal principal) {
        log.info("litst request: " + pageRequestDTO);
        model.addAttribute("result", pdsService.getList(pageRequestDTO));

        if (principal != null) {
            model.addAttribute("mid", principal.getName());
        }

        return "pds/pdsList";
    }

    @GetMapping("/pdsInput")
    public String pdsInputGet(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        if (!request.isUserInRole("ROLE_ADMIN")) {
            redirectAttributes.addFlashAttribute("message", "관리자만 접근할 수 있습니다.");
            return "redirect:/pds/pdsList";
        }
        return "pds/pdsInput";
    }

    @PostMapping("/pdsInput")
    public String pdsInputPost(PdsDto pdsDto,
                               @RequestParam("uploadFiles") List<MultipartFile> uploadFiles,
                               RedirectAttributes redirectAttributes,
                               Principal principal) {

        if (principal == null) return "redirect:/member/memberLogin";

        pdsDto.setMid(principal.getName());
        pdsService.setPdsInput(pdsDto, uploadFiles);
        redirectAttributes.addFlashAttribute("message", "자료가 등록되었습니다.");
        return "redirect:/pds/pdsList";
    }

    @GetMapping("/pdsContent")
    public String pdsContentGet(Long id, Model model,
                                @ModelAttribute("requestDTO") PageRequestDTO pageRequestDTO,
                                HttpServletRequest request,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        String email = (principal != null) ? principal.getName() : "guest";
        PdsDto dto = pdsService.getPdsContent(id, request.getSession(), email);

        if ("NO".equals(dto.getOpenSw())) {
            if (!email.equals(dto.getMid()) && !request.isUserInRole("ROLE_ADMIN")) {
                redirectAttributes.addFlashAttribute("message", "비공개 게시글입니다.");
                return "redirect:/pds/pdsList?page=" + pageRequestDTO.getPage();
            }
        }

        model.addAttribute("dto", dto);
        model.addAttribute("email", email);
        model.addAttribute("replyList", pdsService.getReplyList(id));

        return "pds/pdsContent";
    }

    @GetMapping("/pdsUpdate")
    public String pdsUpdateGet(Long id, Model model,
                               @ModelAttribute("requestDTO") PageRequestDTO pageRequestDTO,
                               HttpServletRequest request,
                               Principal principal) {

        if (principal == null) return "redirect:/member/memberLogin";

        PdsDto dto = pdsService.getPdsContent(id, request.getSession(), principal.getName());

        if (!dto.getMid().equals(principal.getName()) && !request.isUserInRole("ROLE_ADMIN")) {
            return "redirect:/pds/pdsContent?id=" + id;
        }

        model.addAttribute("dto", dto);
        return "pds/pdsUpdate";
    }

    @PostMapping("/pdsUpdate")
    public String pdsUpdatePost(PdsDto pdsDto,
                                @ModelAttribute("requestDTO") PageRequestDTO pageRequestDTO,
                                @RequestParam(value = "uploadFiles", required = false) List<MultipartFile> uploadFiles,
                                @RequestParam(value = "deleteFiles", required = false) List<Long> deleteFileIds,
                                RedirectAttributes redirectAttributes) {

        pdsService.setPdsUpdate(pdsDto, uploadFiles, deleteFileIds);

        redirectAttributes.addFlashAttribute("message", "자료가 수정되었습니다.");
        redirectAttributes.addAttribute("page", pageRequestDTO.getPage());
        redirectAttributes.addAttribute("type", pageRequestDTO.getType());
        redirectAttributes.addAttribute("keyword", pageRequestDTO.getKeyword());
        redirectAttributes.addAttribute("id", pdsDto.getId());

        return "redirect:/pds/pdsContent";
    }

    @PostMapping("/delete")
    public String delete(Long id, RedirectAttributes redirectAttributes) {
        pdsService.delete(id);
        redirectAttributes.addFlashAttribute("message", "자료가 삭제되었습니다.");
        return "redirect:/pds/pdsList";
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long fileId) {
        return pdsService.downloadFile(fileId);
    }

    @GetMapping("/vote/{id}")
    public String vote(@PathVariable("id") Long id,
                       @RequestParam("type") String type,
                       HttpServletRequest request,
                       Principal principal,
                       RedirectAttributes redirectAttributes) {

        String mid = (principal != null) ? principal.getName() : "guest";
        boolean success = pdsService.setGoodBad(id, type, request.getSession(), mid);

        if (success) redirectAttributes.addFlashAttribute("message", "반영되었습니다.");
        else redirectAttributes.addFlashAttribute("message", "이미 참여하셨습니다.");

        return "redirect:/pds/pdsContent?id=" + id;
    }

    @PostMapping("/reply/register")
    public String replyRegister(PdsReplyDto replyDto,
                                @RequestParam(required = false) Long parentId,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {

        if (principal == null) return "redirect:/member/memberLogin";

        replyDto.setMid(principal.getName());
        replyDto.setParentId(parentId);
        pdsService.saveReply(replyDto);

        if (parentId == null) {
            redirectAttributes.addFlashAttribute("message", "댓글이 등록되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("message", "답글이 등록되었습니다.");
        }

        return "redirect:/pds/pdsContent?id=" + replyDto.getPdsId();
    }

    @PostMapping("/reply/update")
    public String replyUpdate(PdsReplyDto replyDto, RedirectAttributes redirectAttributes) {
        pdsService.updateReply(replyDto);
        redirectAttributes.addFlashAttribute("message", "댓글이 수정되었습니다.");
        return "redirect:/pds/pdsContent?id=" + replyDto.getPdsId();
    }

    @PostMapping("/reply/delete")
    public String replyDelete(Long replyId, Long pdsId, RedirectAttributes redirectAttributes) {
        pdsService.deleteReply(replyId);
        redirectAttributes.addFlashAttribute("message", "댓글이 삭제되었습니다.");
        return "redirect:/pds/pdsContent?id=" + pdsId;
    }

}