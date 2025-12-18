package com.example.springGroupBA.common;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.member.MessageDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.member.Message;
import com.example.springGroupBA.repository.member.AdminMemberRepository;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.member.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class Pagination {

  private final MemberRepository memberRepository;
  private final AdminMemberRepository adminMemberRepository;
  private final MessageRepository webMessageRepository;

  public PageVO pagination(PageVO pageVO) {

    // 페이지 보정
    int pag = pageVO.getPag() < 1 ? 1 : pageVO.getPag();
    int pageSize = pageVO.getPageSize() == 0 ? 10 : pageVO.getPageSize();
    int pageIndex = pag - 1;

    PageRequest pageable =
            PageRequest.of(pageIndex, pageSize, Sort.by("id").descending());

    int totRecCnt = 0;
    int totPage = 0;

    /* ================= 회원 ================= */
    if ("member".equals(pageVO.getSection())) {
      boolean isSearch =
              pageVO.getSearchString() != null &&
                      !pageVO.getSearchString().isBlank();

      Page<Member> page;

      if (!isSearch) {
        page = memberRepository.findAll(pageable);
      }
      else {
        switch (pageVO.getSearch()) {
          case "mid":
            page = adminMemberRepository.findByMidContaining(
                    pageVO.getSearchString(), pageable);
            break;

          case "name":
            page = adminMemberRepository.findByNameContaining(
                    pageVO.getSearchString(), pageable);
            break;

          case "email":
            page = adminMemberRepository.findByEmailContaining(
                    pageVO.getSearchString(), pageable);
            break;

          default:
            page = memberRepository.findAll(pageable);
        }
      }

      pageVO.setMemberList(page.getContent());
      totRecCnt = (int) page.getTotalElements();
      totPage = page.getTotalPages();
    }
    /* ================= 회원 등급 ================= */
    else if ("memberGrade".equals(pageVO.getSection())) {

      boolean hasKeyword = pageVO.getKeyword() != null && !pageVO.getKeyword().isBlank();
      boolean hasRole = pageVO.getRole() != null && !pageVO.getRole().isBlank();

      Page<Member> page;

      if (hasKeyword && hasRole) {
        Role r = Role.valueOf(pageVO.getRole());
        switch (pageVO.getSearch()) {
          case "mid":
            page = adminMemberRepository.findByMidContainingAndRole(pageVO.getKeyword(), r, pageable);
            break;
          case "name":
            page = adminMemberRepository.findByNameContainingAndRole(pageVO.getKeyword(), r, pageable);
            break;
          case "email":
            page = adminMemberRepository.findByEmailContainingAndRole(pageVO.getKeyword(), r, pageable);
            break;
          default:
            page = memberRepository.findAll(pageable);
        }
      } else if (hasKeyword) {
        switch (pageVO.getSearch()) {
          case "mid":
            page = adminMemberRepository.findByMidContaining(pageVO.getKeyword(), pageable);
            break;
          case "name":
            page = adminMemberRepository.findByNameContaining(pageVO.getKeyword(), pageable);
            break;
          case "email":
            page = adminMemberRepository.findByEmailContaining(pageVO.getKeyword(), pageable);
            break;
          default:
            page = memberRepository.findAll(pageable);
        }
      } else if (hasRole) {
        Role r = Role.valueOf(pageVO.getRole());
        page = adminMemberRepository.findByRole(r, pageable);
      } else {
        page = memberRepository.findAll(pageable);
      }

      pageVO.setMemberList(page.getContent());
      totRecCnt = (int) page.getTotalElements();
      totPage = page.getTotalPages();
    }
    /* ================= 탈퇴 회원 ================= */
    else if ("memberDel".equals(pageVO.getSection())) {
      PageRequest pageableDel =
              PageRequest.of(pageIndex, pageSize, Sort.by("deleteDate").descending());

      Page<Member> page =
              (pageVO.getSearchString() == null || pageVO.getSearchString().isBlank())
                      ? adminMemberRepository.findByUserDel(UserDel.OK, pageableDel)
                      : adminMemberRepository.findByUserDelAndNickNameContaining(
                      UserDel.OK,
                      pageVO.getSearchString(),
                      pageableDel
              );

      pageVO.setMemberList(page.getContent());
      totRecCnt = (int) page.getTotalElements();
      totPage = page.getTotalPages();
    }

    /* ================= 웹 메시지 ================= */
    else if ("webMessage".equals(pageVO.getSection())) {
      boolean isSearch =
              pageVO.getSearchString() != null &&
                      !pageVO.getSearchString().isBlank();

      Page<Message> webMessages =
              isSearch
                      ? webMessageRepository.searchAllMessages(
                      pageVO.getSearch(),
                      pageVO.getSearchString(),
                      pageable
              )
                      : webMessageRepository.findAllMessages(pageable);

      List<MessageDto> dtoList =
              webMessages.getContent()
                      .stream()
                      .map(MessageDto::entityToDto)
                      .toList();

      pageVO.setWebMessageList(dtoList);
      totRecCnt = (int) webMessages.getTotalElements();
      totPage = webMessages.getTotalPages();
    }

    /* ================= 페이지 계산 ================= */
    int startIndexNo = (pag - 1) * pageSize;
    int curScrStartNo = totRecCnt - startIndexNo;

    int blockSize = 3;
    int curBlock = (pag - 1) / blockSize;
    int lastBlock = (totPage == 0) ? 0 : (totPage - 1) / blockSize;

    int startPageNo = (totPage == 0) ? 1 : curBlock * blockSize + 1;
    int endPageNo = (totPage == 0)
            ? 1
            : Math.min(startPageNo + blockSize - 1, totPage);

    /* ================= PageVO 세팅 ================= */
    pageVO.setPag(pag);
    pageVO.setPageSize(pageSize);
    pageVO.setTotRecCnt(totRecCnt);
    pageVO.setTotPage(totPage);
    pageVO.setStartIndexNo(startIndexNo);
    pageVO.setCurScrStartNo(curScrStartNo);
    pageVO.setBlockSize(blockSize);
    pageVO.setCurBlock(curBlock);
    pageVO.setLastBlock(lastBlock);
    pageVO.setStartPageNo(startPageNo);
    pageVO.setEndPageNo(endPageNo);

    return pageVO;
  }
}
