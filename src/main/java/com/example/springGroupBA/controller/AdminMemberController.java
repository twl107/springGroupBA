package com.example.springGroupBA.controller;

import com.example.springGroupBA.common.PageVO;
import com.example.springGroupBA.common.Pagination;
import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.dto.member.MessageDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.service.AdminMemberService;
import com.example.springGroupBA.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;


@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberController {
  private final AdminMemberService adminMemberService;
  private final MessageService messageService;
  private final Pagination pagination;

  // 회원 목록 폼
  @GetMapping("/list")
  public String memberListGet(
          @RequestParam(value = "page", defaultValue = "1") int page,
          @RequestParam(value = "keyword", required = false) String keyword,
          @RequestParam(value = "search", defaultValue = "mid") String search,
          Model model) {

    PageVO pageVO = new PageVO();
    pageVO.setPag(page);
    pageVO.setPageSize(10);
    pageVO.setSearch(search);
    pageVO.setSearchString(keyword);
    pageVO.setSection("member");

    pageVO = pagination.pagination(pageVO);

    model.addAttribute("pageVO", pageVO);
    return "admin/member/adminMemberList";
  }

  // 계정 상태 토글
  @GetMapping("/toggle/{id}")
  public String toggleMemberStatus(@PathVariable Long id, RedirectAttributes rttr) {
    boolean success = adminMemberService.getToggleMemberDel(id);

    if (!success) {
      rttr.addFlashAttribute("message", "회원정보가 존재하지 않습니다.");
    } else {
      rttr.addFlashAttribute("message", "회원 상태가 변경되었습니다.");
    }

    return "redirect:/admin/members/list";
  }

  // 등급 관리 폼
  @GetMapping("/grade")
  public String memberGradeGet(
          @RequestParam(value = "page", defaultValue = "1") int page,
          @RequestParam(value = "keyword", required = false) String keyword,
          @RequestParam(value = "role", required = false) String role,
          @RequestParam(value = "search", defaultValue = "mid") String search,
          Model model) {

    PageVO pageVO = new PageVO();
    pageVO.setPag(page);
    pageVO.setPageSize(10);

    // keyword + role 조합 처리
    pageVO.setKeyword(keyword);
    pageVO.setRole(role);
    pageVO.setSearch(search);

    pageVO.setSection("memberGrade");
    pageVO.setCurrentPage("grade");
    pageVO = pagination.pagination(pageVO);

    model.addAttribute("pageVO", pageVO);
    model.addAttribute("keyword", keyword);
    model.addAttribute("role", role);

    return "admin/member/adminMemberGrade";
  }

  // 회원 등급 변경처리
  @PostMapping("/gradeChange/{id}")
  public String gradeChangePost(
          @PathVariable Long id,
          @RequestParam("newRole") String newRole,
          RedirectAttributes attr) {

    Optional<Member> memberOpt = adminMemberService.getMemberById(id);
    if (memberOpt.isEmpty()) {
      attr.addFlashAttribute("message", "회원 정보를 찾을 수 없습니다.");
      return "redirect:/admin/members/grade";
    }

    Member member = memberOpt.get();
    try {
      member.setRole(Role.valueOf(newRole));
    } catch (IllegalArgumentException e) {
      attr.addFlashAttribute("message", "유효하지 않은 등급입니다.");
      return "redirect:/admin/members/grade";
    }

    adminMemberService.getGradeUpdate(member);
    attr.addFlashAttribute("message", member.getNickName() + "님의 등급이 " + newRole + "로 변경되었습니다.");

    return "redirect:/admin/members/grade";
  }

  // 회원탈퇴 폼
  @GetMapping("/memberDel")
  public String memberDelGet(
          @RequestParam(value = "page", defaultValue = "1") int page,
          @RequestParam(value = "keyword", required = false) String keyword,
          @RequestParam(value = "search", defaultValue = "mid") String search,
          Model model) {

    PageVO pageVO = new PageVO();

    pageVO.setPag(page);
    pageVO.setPageSize(10);
    pageVO.setSearch(search);
    pageVO.setSearchString(keyword);
    pageVO.setSection("memberDel");
    pageVO.setCurrentPage("memberDel");

    pageVO = pagination.pagination(pageVO);

    model.addAttribute("pageVO", pageVO);

    return "admin/member/adminMemberDel";
  }

  // 회원삭제 처리
  @PostMapping("/memberDel/{id}")
  public String memberDelPost(@PathVariable Long id, RedirectAttributes rttr) {
    Optional<Member> memberOpt = adminMemberService.getMemberById(id);

    if (memberOpt.isEmpty()) {
      rttr.addFlashAttribute("message", "회원 정보를 찾을 수 없습니다.");
      return "redirect:/admin/members/memberDel";
    }

    Member member = memberOpt.get();
    String mid = member.getMid();

    boolean success = adminMemberService.getMemberDelete(id);

    if (!success) {
      rttr.addFlashAttribute("message", "회원 삭제에 실패했습니다.");
      return "redirect:/admin/members/memberDel";
    }

    rttr.addFlashAttribute("message", "탈퇴중인 회원(ID: " + mid + ")을 삭제하였습니다.");

    return "redirect:/admin/members/memberDel";
  }

  // 메일 폼
  @GetMapping("/mail")
  public String memberMailGet(
          @RequestParam(defaultValue = "1") int page,
          @RequestParam(required = false) String keyword,
          @RequestParam(defaultValue = "title") String search,
          Model model) {

    PageVO pageVO = new PageVO();
    pageVO.setPag(page);
    pageVO.setPageSize(10);
    pageVO.setSection("webMessage");
    pageVO.setSearch(search);
    pageVO.setSearchString(keyword);
    pageVO.setCurrentPage("webMessageList");

    pageVO = pagination.pagination(pageVO);

    model.addAttribute("pageVO", pageVO);
    return "admin/member/adminMemberMail";
  }

  // 상세메세지 모달
  @GetMapping("/mail/detail/{id}")
  @ResponseBody
  public ResponseEntity<MessageDto> getMessageDetail(@PathVariable Long id) {
    return messageService.findById(id)
            .map(MessageDto::entityToDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
  }

  // 메세지 삭제
  @PostMapping("/mail/delete/{id}")
  @ResponseBody
  public ResponseEntity<String> deleteMessage(@PathVariable Long id) {
    try {
      messageService.deleteMessage(id);
      return ResponseEntity.ok("메세지가 삭제되었습니다.");
    } catch (Exception e) {
      log.error("메시지 삭제 실패: {}", e.getMessage(), e);
      return ResponseEntity.status(500).body("삭제 실패");
    }
  }
}
