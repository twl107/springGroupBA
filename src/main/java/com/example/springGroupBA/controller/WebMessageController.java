package com.example.springGroupBA.controller;

import com.example.springGroupBA.common.PageVO;
import com.example.springGroupBA.common.Pagination;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.member.MessageDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.member.Message;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.member.MessageRepository;
import com.example.springGroupBA.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/webMessage")
public class WebMessageController {

  private final MessageRepository webMessageRepository;
  private final MessageService messageService;
  private final MemberRepository memberRepository;

  // 새 메세지 갯수
  @GetMapping("/newMsgCount")
  @ResponseBody
  public Map<String, Long> getNewMsgCount(Authentication authentication) {
    String email = authentication.getName();
    long count = webMessageRepository.countByMemberReceiveId_EmailAndReceiveSw(email, "n");
    return Map.of("count", count);
  }

  @GetMapping("/msg")
  public String webMessageGet(Model model, Authentication authentication, RedirectAttributes rttr,
                              @RequestParam(name="receiveId", defaultValue = "1", required = false) String receiveId,
                              @RequestParam(name="msgSw", defaultValue = "1", required = false) int msgSw,
                              @RequestParam(name="preSw", defaultValue = "1", required = false) int preSw,
                              @RequestParam(name="pag", defaultValue = "1", required = false) int pag,
                              @RequestParam(name="pageSize", defaultValue = "10", required = false) int pageSize,
                              @RequestParam(name="id", defaultValue = "0", required = false) Long id
  ) {
    String mid = authentication.getName(); // 로그인 이메일
    PageVO pageVO = new PageVO();
    int currentPage = Math.max(pag - 1, 0);
    pageVO.setPag(pag);
    pageVO.setPageSize(pageSize);
    pageVO.setMsgSw(msgSw);
    pageVO.setPreSw(preSw);
    pageVO.setSection("webMessage");

    long newMsgCount = webMessageRepository.countByMemberReceiveId_EmailAndReceiveSw(mid, "n");
    model.addAttribute("newMsgCount", newMsgCount);

    // 1. 메시지 작성
    if (msgSw == 0) {
      List<Member> memberList = memberRepository.findByUserDel(UserDel.NO);
      pageVO.setMsgSw(0);
      model.addAttribute("memberList", memberList);

      if (id != null && id != 0) { // 답장
        webMessageRepository.findById(id).ifPresentOrElse(replyMessage -> {
          model.addAttribute("webMessage", replyMessage);
          if (replyMessage.getMemberSendId() != null && replyMessage.getMemberSendId().getEmail() != null) {
            model.addAttribute("receiveId", replyMessage.getMemberSendId().getEmail());
          } else {
            model.addAttribute("receiveId", "");
          }
        }, () -> {
          model.addAttribute("receiveId", "");
          model.addAttribute("replyNotFound", true);
        });
      }
    }
    // 2. 수신확인 (msgSw=4)
    else if (msgSw == 4) {
      List<Message> allMessages = webMessageRepository.findByMessageRead(mid, List.of("n", "r"));
      int total = allMessages.size();
      pageVO.setTotRecCnt(total);

      int start = currentPage * pageSize;
      int end = Math.min(start + pageSize, total);
      List<Message> pageList = allMessages.subList(start, end);

      pageVO.setWebMessageList(pageList);
      pageVO.setCurScrStartNo(total - currentPage * pageSize);
      pageVO.setTotPage((int) Math.ceil((double) total / pageSize));
    }

    // 3. 메시지 내용보기 (msgSw=6)
    else if (msgSw == 6) {
      Message webMessage = messageService.readMessage(id, mid);
      webMessage.setContent(webMessage.getContent().replace("\n", "<br/>"));
      model.addAttribute("webMessage", webMessage);
    }

    // 4. 휴지통 비우기 (msgSw=9)
    else if (msgSw == 9) {
      List<Message> wasteList = webMessageRepository.findByMemberReceiveId_EmailAndReceiveSw(mid, "g");
      if (!wasteList.isEmpty()) {
        wasteList.forEach(msg -> msg.setReceiveSw("x"));
        webMessageRepository.saveAll(wasteList);
        webMessageRepository.deleteByReceiveSwAndSendSw("x", "x");
        rttr.addFlashAttribute("message", "휴지통 메세지가 영구적으로 삭제되었습니다.");
      }
      return "redirect:/webMessage/msg?msgSw=5";
    }

    // 5. 나머지 페이지 (받은메시지, 새메시지, 보낸메시지, 휴지통)
    else {
      PageRequest pageable = PageRequest.of(currentPage, pageSize, Sort.by(Sort.Direction.DESC, "id"));
      Page<Message> messagePage;

      messagePage = switch (msgSw) {
        case 1 -> webMessageRepository.findReceivedMessages(mid, pageable);
        case 2 -> webMessageRepository.findNewMessages(mid, pageable);
        case 3 -> webMessageRepository.findSendMessages(mid, pageable);
        case 5 -> webMessageRepository.findWasteBasketMessages(mid, pageable);
        default -> Page.empty();
      };

      pageVO.setWebMessageList(messagePage.getContent());
      pageVO.setTotRecCnt((int) messagePage.getTotalElements());
      pageVO.setCurScrStartNo((int) (messagePage.getTotalElements() - currentPage * pageSize));
      pageVO.setTotPage(messagePage.getTotalPages());
      System.out.println("msgSW(000) :" + msgSw);
    }
    model.addAttribute("pageVO", pageVO);
    System.out.println("msgSW :" + msgSw);

    return "member/webMessage/message";
  }

  // 메세지 저장처리
  @PostMapping("/wmInputOk")
  public String wmInputOkPost(MessageDto dto, RedirectAttributes rttr) {
    Member memberSenderEmail = memberRepository.findByEmail(dto.getSendId())
            .orElseThrow(() -> new IllegalArgumentException("보낸 사람을 찾을 수 없습니다: " + dto.getSendId()));

    Member memberReceiveEmail = memberRepository.findByEmail(dto.getReceiveId())
            .orElseThrow(() -> new IllegalArgumentException("받는 사람을 찾을 수 없습니다: " + dto.getReceiveId()));

    Message webMessage = Message.dtoToEntity(dto, memberSenderEmail, memberReceiveEmail);
    webMessage.setReceiveDate(LocalDateTime.now()); // 작성 시점 수신일시

    webMessageRepository.save(webMessage);
    rttr.addFlashAttribute("message", "메세지가 성공적으로 전송되었습니다.");

    return "redirect:/webMessage/msg?msgSw=1";
  }

  // 메세지 삭제처리
  @RequestMapping(value = "/webDeleteCheck", method = RequestMethod.GET)
  public String webMessageDeleteOkGet(Long id, int msgSw, int preSw, RedirectAttributes rttr) {
    Message webMessage = webMessageRepository.findById(id).orElseThrow();

    String alertMessage = "";

    if(msgSw == 5) { // 받은메시지에서 휴지통 이동
      webMessage.setReceiveSw("g");
      alertMessage = "메세지가 휴지통으로 이동되었습니다.";
    } else if (msgSw == 3) { // 보낸메시지에서 영구삭제
      webMessage.setSendSw("x");
      alertMessage = "보낸 메세지가 영구적으로 삭제되었습니다.";
    }

    webMessageRepository.save(webMessage);
    rttr.addFlashAttribute("message", alertMessage);

    // 삭제 후 원래 페이지(preSw)로 돌아가기
    return "redirect:/webMessage/msg?msgSw=" + preSw;
  }
}
