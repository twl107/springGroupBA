package com.example.springGroupBA.controller;

import com.example.springGroupBA.common.ProjectProvide;
import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.*;
import com.example.springGroupBA.dto.member.*;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.member.Message;
import com.example.springGroupBA.entity.survey.SurveyMeta;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.member.MessageRepository;
import com.example.springGroupBA.service.KakaoService;
import com.example.springGroupBA.service.MemberService;
import com.example.springGroupBA.service.SurveyService;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

  private final MemberService memberService;
  private final ProjectProvide projectProvide;
  private final PasswordEncoder passwordEncoder;
  private final KakaoService kakaoService;
  private final MemberRepository memberRepository;
  private final MessageRepository messageRepository;
  private final SurveyService surveyService;

  @GetMapping("/")
  public String homeGet() {
    return "home";
  }

  // 로그인 폼
  @GetMapping("/memberLogin")
  public String memberLoginGet(Model model, HttpSession session) {
    String kakaoUrl = kakaoService.getKakaoLogin();
    model.addAttribute("kakaoUrl", kakaoUrl);

    // 세션에서 에러 메시지 가져오기
    String msg = (String) session.getAttribute("message");
    if(msg != null) {
      model.addAttribute("message", msg);
      session.removeAttribute("message");
    }

    return "member/memberLogin";
  }

  // 로그인 처리
  @GetMapping("/memberLoginOk")
  public String memberLoginOkGet(
          RedirectAttributes rttr,
          Authentication authentication,
          HttpSession session
  ) {
    String email = authentication.getName();
    Optional<Member> opMember = memberService.getMemberByEmail(email);

    if(opMember.isEmpty()) {
      rttr.addFlashAttribute("message", "회원 정보를 찾을 수 없습니다.");
      return "redirect:/member/memberLogin";
    }

    Member member = opMember.get();
    session.setAttribute("loginMember", member);
    session.setAttribute("loginType", "normal");

    rttr.addFlashAttribute("message", member.getMid() + "님 로그인 성공");

    return "redirect:/member/loginMain";
  }

  // 카카오 로그인
  @GetMapping("/kakaoLogin")
  public String kakaoLoginGet(HttpServletRequest request, HttpSession session, Model model) throws Exception {
    KakaoDto kakaoInfo = kakaoService.getKakaoInfo(request.getParameter("code"), session);

    Member member = null;
    if(kakaoInfo.getEmail() != null) {
      member = memberRepository.findByEmail(kakaoInfo.getEmail()).orElse(null);
    }

    // 탈퇴 메세지
    if(member != null && member.getUserDel() == UserDel.OK) {
      model.addAttribute("message", "해당 계정은 탈퇴 처리중 입니다.\n탈퇴신청 날짜부터 7일 이후 재가입 가능합니다.");
      model.addAttribute("redirectUrl", "/springGroupBA/member/memberLogin");
      return "member/popup/loginPopup";
    }

    // SecurityContext 설정
    String username = kakaoInfo.getEmail() != null ? kakaoInfo.getEmail() : "kakao_" + kakaoInfo.getId();
    UserDetails userDetails = User.builder()
            .username(username)
            .password("")
            .roles("USER")
            .build();

    Authentication authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(authToken);

    // 세션에 로그인 정보 저장
    session.setAttribute("loginMember", member);
    session.setAttribute("loginType", "kakao");
    session.setAttribute("kakaoNickName", kakaoInfo.getNickName());
    session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
    );

    // 브라우저 종료 시 세션 종료
    session.setMaxInactiveInterval(30*60);

    // 팝업에 메시지 + 부모 창 리다이렉트
    model.addAttribute("message", kakaoInfo.getEmail() != null ? kakaoInfo.getEmail() + "님 로그인 성공" : "user 로그인 성공");
    model.addAttribute("redirectUrl", "/springGroupBA/member/memberMain");

    return "member/popup/loginPopup";
  }

  // 로그아웃
  @GetMapping("/memberLogout")
  public String memberLogout(Authentication authentication, HttpSession session, HttpServletResponse response, RedirectAttributes rttr) {

    Member loginMember = (Member) session.getAttribute("loginMember");
    String loginType = (String) session.getAttribute("loginType");

    String name = "user";
    if (loginMember != null) {
      name = loginMember.getNickName();

      // 관리자일 경우
      if ("ADMIN".equalsIgnoreCase(loginMember.getRole().name())) {
        name = "관리자";
      }
    }

    // 카카오 로그아웃 처리
    if("kakao".equals(loginType)) {
      try {
        kakaoService.kakaoLogout(session);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // SecurityContext + 세션 초기화
    SecurityContextHolder.clearContext();
    if(session != null) session.invalidate();

    // remember-me 쿠키 삭제
    Cookie cookie = new Cookie("remember-me", null);
    cookie.setPath("/");
    cookie.setMaxAge(0);
    response.addCookie(cookie);

    rttr.addFlashAttribute("message", name + "님 로그아웃 성공");
    return "redirect:/member/memberLogin";
  }

  // 에러
  @GetMapping("/error")
  public String loginErrorGet(RedirectAttributes rttr) {
    rttr.addFlashAttribute("loginErrorMsg", "아이디 또는 비밀번호가 일치하지 않습니다.");
    return "redirect:/member/memberLogin";
  }

  // 회원가입 폼
  @GetMapping("/memberJoin")
  public String memberJoinForm(Model model) {
    model.addAttribute("memberDto", new MemberDto());
    model.addAttribute("photoUrl", "/resources/static/images/noimage.jpg");

    return "member/memberJoin";
  }

  // 회원가입 처리
  @PostMapping("/memberJoin")
  public String memberJoinPost(@Valid MemberDto dto, BindingResult bindingResult, Model model, RedirectAttributes rttr) {
    if (bindingResult.hasErrors()) {
      return "member/memberJoin";
    }

    String email1 = dto.getEmail1();
    String email2 = dto.getEmail2();
    String email = email1 + "@" + email2;

    if (email1 == null || email1.isEmpty() || email2 == null || email2.isEmpty()) {
      bindingResult.rejectValue("email1", "error.email", "이메일은 필수입력 입니다.");
    }
    else {
      if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
        bindingResult.rejectValue("email1", "error.email", "이메일 형식이 올바르지 않습니다.");
      }
    }

    String tel1 = dto.getTel1();
    String tel2 = dto.getTel2();
    String tel3 = dto.getTel3();

    if (tel1 == null || tel1.isEmpty() || tel2 == null || tel2.isEmpty() || tel3 == null || tel3.isEmpty()) {
      bindingResult.rejectValue("tel1", "error.tel", "전화번호는 필수입력입니다.");
    } else {
      String tel = tel1 + "-" + tel2 + "-" + tel3;
      if (!tel.matches("^\\d{2,3}-\\d{3,4}-\\d{4}$")) {
        bindingResult.rejectValue("tel1", "error.tel", "전화번호 형식이 올바르지 않습니다.");
      }
    }

    // 아이디 중복
    if (memberService.getMemberIdCheck(dto.getMid())) {
      bindingResult.rejectValue("mid", "error.mid", "사용 중인 아이디입니다.");
      return "member/memberJoin";
    }

    // 닉네임 중복
    if (memberService.getMemberNickNameCheck(dto.getNickName())) {
      bindingResult.rejectValue("nickName", "error.nickName", "사용 중인 닉네임입니다.");
      return "member/memberJoin";
    }

    // 메일 인증 확인
    Boolean emailVerified = (Boolean) model.getAttribute("emailVerified");
    if (emailVerified == null || !emailVerified) {
      bindingResult.rejectValue("email1", "error.email", "이메일 인증이 필요합니다.");
    }

    // 이메일 중복 체크
    Optional<Member> existingMember = memberService.getMemberByEmail(email);
    if (existingMember.isPresent()) {
      model.addAttribute("message", "이미 사용 중인 이메일입니다.");
      return "member/memberJoin";
    }

    // 비밀번호 확인
    if (!dto.getPassword().equals(dto.getPasswordCheck())) {
      bindingResult.rejectValue("passwordCheck", "error.passwordCheck", "비밀번호가 일치하지 않습니다.");
      return "member/memberJoin";
    }

    // 아이디 탈퇴 후 7일 제한 체크
    try {
      memberService.validateJoinByMid(dto.getMid());
    } catch (IllegalArgumentException e) {
      bindingResult.rejectValue("mid", "error.mid", e.getMessage());
      return "member/memberJoin";
    }

    try {
      Member member = Member.dtoToEntity(dto, passwordEncoder);
      memberService.joinMember(member, dto.getFile());
      rttr.addFlashAttribute("message", "회원가입 완료!");

      return "redirect:/member/memberLogin";

    } catch (Exception e) {
      model.addAttribute("message", "회원가입 실패: " + e.getMessage());
      return "member/memberJoin";
    }
  }

  // 아이디 체크
  @ResponseBody
  @PostMapping("/idCheck")
  public boolean userIdCheck(String mid) {
    return memberService.getMemberIdCheck(mid);
  }

  // 닉네임 체크
  @ResponseBody
  @PostMapping("/nickNameCheck")
  public boolean userNickNameCheck(String nickName) {
    return memberService.getMemberNickNameCheck(nickName);
  }

  // 회원가입시 이메일로 인증번호 전송하기
  @ResponseBody
  @PostMapping("/memberEmailCheck")
  public int memberEmailCheckPost(String email, HttpSession session) throws MessagingException {
    String emailKey = UUID.randomUUID().toString().substring(0, 8);

    // 이메일 인증키를 세션에 저장시켜둔다.(2분안에 인증하지 않으면 다시 발행해야함...)
    session.setAttribute("sEmailKey", emailKey);

    projectProvide.mailSend(email, "이메일 인증키입니다.", "이메일 인증키 : " + emailKey);

    return 1;
  }

  // 이메일로 인증번호받은 인증키 확인하기(가입/수정)
  @ResponseBody
  @PostMapping("/memberEmailCheckOk")
  public int memberEmailCheckOkPost(String checkKey, HttpSession session) {
    String emailKey = (String) session.getAttribute("sEmailKey");

    if (emailKey != null && checkKey.equals(emailKey)) {
      session.removeAttribute("sEmailKey");

      // 회원정보 수정 시 인증 완료 표시
      Boolean isUpdateEmail = (Boolean) session.getAttribute("isUpdateEmail");
      if (isUpdateEmail != null && isUpdateEmail) {
        session.setAttribute("updateEmailVerified", true);
        session.removeAttribute("isUpdateEmail");
      }

      return 1;
    }
    return 0;
  }

  // 이메일 인증번호 입력 제한시간(2분)안에 인증확인하지 못하면 발행한 인증번호 삭제하기
  @ResponseBody
  @PostMapping("/memberEmailCheckNo")
  public void memberEmailCheckNoPost(HttpSession session) {
    // 회원가입 세션
    session.removeAttribute("sEmailKey");

    // 아이디 찾기 세션
    session.removeAttribute("findMidCode");
    session.removeAttribute("findMidEmail");
    session.removeAttribute("findMidCodeTime");
  }

  // 회원 정보 폼
  @GetMapping("/memberMain")
  public String memberMain(HttpSession session, Authentication authentication, Model model) {
    Member member = memberService.getMemberByEmail(authentication.getName())
            .orElseThrow(() -> new UsernameNotFoundException("로그인한 회원이 존재하지 않습니다."));

    // 프로필
    String photoFileName = "noimage.jpg";
    if(member.getPhotoName() != null && !member.getPhotoName().equals("noimage.jpg")) {
      photoFileName = URLEncoder.encode(member.getPhotoName(), StandardCharsets.UTF_8);
    }
    model.addAttribute("photoFileName", photoFileName);
    model.addAttribute("loginMember", member);

    // 메시지
    model.addAttribute("newMsgList", messageRepository.findTop3ByMemberReceiveIdEmailAndReceiveSwAndReadDateIsNullOrderByReceiveDateDesc(member.getEmail(), "n"));
    model.addAttribute("receiveMsgList", messageRepository.findTop3ByMemberReceiveIdEmailAndReceiveSwNotAndReadDateIsNotNullOrderByReadDateDesc(member.getEmail(), "g"));
    model.addAttribute("sendMsgList", messageRepository.findTop3ByMemberSendIdEmailAndSendSwNotOrderBySendDateDesc(member.getEmail(), "x"));

    String photoUrl = "/images/noimage.jpg";
    if (member.getPhotoName() != null && !member.getPhotoName().equals("noimage.jpg")) {
      photoUrl = "/upload/member/" + URLEncoder.encode(member.getPhotoName(), StandardCharsets.UTF_8);
    }
    model.addAttribute("photoUrl", photoUrl);

    model.addAttribute("loginMember", member);

    // 알림 메시지
    Object msg = session.getAttribute("message");
    if (msg != null) {
      model.addAttribute("message", msg);
      session.removeAttribute("message");
    }

    // ==========================================================
    // 🔹 설문 처리 (핵심 변경)
    // ==========================================================

    // 1) 활성 설문 목록 가져오기
    List<SurveyMeta> activeSurvey = surveyService.findActiveMeta();

    // 2) 설문별 개별 참여 여부 Map 생성
    Map<Long, Boolean> submitMap = new HashMap<>();
    for (SurveyMeta m : activeSurvey) {
      boolean submitted = surveyService.hasSubmitted(member.getId(), m.getId());
      submitMap.put(m.getId(), submitted);
    }

    // 3) View로 전달
    model.addAttribute("surveyList", activeSurvey);
    model.addAttribute("submitMap", submitMap);

    // ==========================================================

    return "member/memberMain";
  }



  // 비밀번호 확인(회원정보 수정/비밀번호 변경)
  @GetMapping("/memberPwdCheck/{flag}")
  public String memberPwdCheckGet(@PathVariable String flag, Model model, Principal principal) {
    String email = principal.getName();
    Member member = memberService.getMemberByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

    model.addAttribute("mid", member.getMid());
    model.addAttribute("flag", flag);

    return "member/memberPwdCheck";
  }

  // 비밀번호 확인 처리
  @PostMapping("/memberPwdCheck")
  @ResponseBody
  public String pwdCheck(@RequestParam String pwd,
                         @RequestParam String flag,
                         Principal principal) {
    String email = principal.getName();
    Member member = memberService.getMemberByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

    // 카카오 로그인은 제외
    if(member.getPassword() == null || member.getPassword().isEmpty()) {
      return flag;
    }

    boolean matches = passwordEncoder.matches(pwd, member.getPassword());
    if(!matches) return "0";

    return flag;
  }

  // 비밀번호 변경 처리
  @PostMapping("/memberPwdChange")
  public String memberPwdChange(@RequestParam String newPwd, Principal principal,
                                HttpServletRequest request, HttpServletResponse response, RedirectAttributes rttr) {
    String mid = principal.getName();
    boolean pwdChange = memberService.changePassword(mid, newPwd);

    if (pwdChange) {
      // 세션 무효화 및 Spring Security 인증 초기화
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null){
        new SecurityContextLogoutHandler().logout(request, response, auth);
      }

      rttr.addFlashAttribute("message", "비밀번호가 변경되어 로그아웃되었습니다. 다시 로그인하세요.");
      return "redirect:/member/memberLogin";
    } else {
      rttr.addFlashAttribute("message", "비밀번호 변경 실패");
      return "redirect:/member/memberPwdCheck/p";
    }
  }

  // 비밀번호 찾기 폼
  @GetMapping("/findPwd")
  public String findPwdGet() {
    return "member/findPwd";
  }

  // 비밀번호 찾기 이메일 전송
  @PostMapping("/findPwd")
  public ResponseEntity<String> findPwdPost(@RequestParam String mid, @RequestParam String email) {
    try {
      memberService.requestPasswordReset(mid, email);
      return ResponseEntity.ok("가입한 이메일로 링크가 전송되었습니다.");
    } catch (IllegalArgumentException e) {
      // 가입된 계정이 아닌 경우
      return ResponseEntity.badRequest().body("아이디 또는 이메일이 일치하지 않습니다.");
    } catch (MessagingException e) {
      return ResponseEntity.internalServerError().body("메일 전송 중 오류가 발생했습니다.");
    }
  }

  // 비밀번호 찾기 새 비밀번호 입력 폼
  @GetMapping("/resetPwd")
  public String resetPwdGet(@RequestParam String token, Model model) {
    boolean valid = memberService.isResetTokenValid(token);
    if (!valid) {
      model.addAttribute("error", "유효하지 않거나 만료된 토큰입니다.");
      return "token_invalid"; // 별도 만료 페이지
    }
    model.addAttribute("token", token);
    return "member/resetPwd";
  }

  // 비밀번호 찾기 비밀번호 변경
  @PostMapping("/resetPwd")
  public ResponseEntity<String> resetPwdPost(@RequestParam String token, @RequestParam String newPwd) {
    memberService.resetPassword(token, newPwd);
    return ResponseEntity.ok("비밀번호가 변경되었습니다.");
  }

  // 아이디 찾기 폼
  @GetMapping("/findMid")
  public String findIdGet() {
    return "member/findMid";
  }

  // 일부 아이디 찾기
  @PostMapping("/findMid")
  @ResponseBody
  public FindMidResponseDto findMidPost(@RequestBody FindMidRequestDto requestDto, HttpServletRequest request) {
    Map<String, String> idsWithEmails = memberService.findIds(requestDto.getEmail(), requestDto.getTel());

    request.getSession().setAttribute("findMidIds", new ArrayList<>(idsWithEmails.keySet()));
    request.getSession().setAttribute("findMidEmails", new ArrayList<>(idsWithEmails.values()));

    return FindMidResponseDto.builder()
            .memberIds(new ArrayList<>(idsWithEmails.keySet()))
            .build();
  }

  // 전체 아이디 조회
  @PostMapping("/findAllIds")
  public FindMidResponseDto findAllIds(@RequestBody FindAllMidRequestDto requestDto, HttpServletRequest request) {
    List<String> allIds = memberService.findAllIdsByEmail(requestDto.getEmail());
    request.getSession().setAttribute("findMidIds", allIds);

    return FindMidResponseDto.builder().memberIds(allIds).build();
  }

  // 아이디찾기 인증코드 이메일 발송
  @PostMapping("/findMid/sendCode")
  public ResponseEntity<Map<String, String>> sendAuthCode(@RequestParam String email, HttpServletRequest request) throws MessagingException {
    HttpSession session = request.getSession();
    List<String> validEmails = (List<String>) session.getAttribute("findMidEmails");
    if (validEmails == null || !validEmails.contains(email)) {
      return ResponseEntity.badRequest().body(Map.of("message", "이메일이 일치하지 않습니다."));
    }

    int code = new Random().nextInt(900000) + 100000;
    projectProvide.mailSend(email, "아이디 전체보기 인증코드", String.valueOf(code));

    session.setAttribute("findMidCode", String.valueOf(code));
    session.setAttribute("findMidEmail", email);
    session.setAttribute("findMidCodeTime", LocalDateTime.now());

    return ResponseEntity.ok(Map.of("message", "인증코드가 전송되었습니다."));
  }

  // 인증코드 확인 후 전체 아이디 보기
  @PostMapping("/findMid/verifyCode")
  public ResponseEntity<FindMidResponseDto> verifyCode(@RequestBody VerifyCodeRequestDto dto,
                                                       HttpServletRequest request) {
    HttpSession session = request.getSession();

    String sessionCode = (String) session.getAttribute("findMidCode");
    String sessionEmail = (String) session.getAttribute("findMidEmail");
    LocalDateTime codeTime = (LocalDateTime) session.getAttribute("findMidCodeTime");

    // 세션 정보가 없으면 만료 메시지
    if (sessionCode == null || sessionEmail == null || codeTime == null) {
      return ResponseEntity.badRequest()
              .body(new FindMidResponseDto(Collections.emptyList(), "인증 코드가 만료되었습니다."));
    }

    // 코드/이메일 불일치
    if (!sessionCode.equals(dto.getCode()) || !sessionEmail.equals(dto.getEmail().trim())) {
      return ResponseEntity.badRequest()
              .body(new FindMidResponseDto(Collections.emptyList(), "인증 실패 또는 이메일 불일치"));
    }

    // 코드 만료
    if (codeTime.plusMinutes(3).isBefore(LocalDateTime.now())) {
      session.removeAttribute("findMidCode");
      session.removeAttribute("findMidEmail");
      session.removeAttribute("findMidCodeTime");

      return ResponseEntity.badRequest()
              .body(new FindMidResponseDto(Collections.emptyList(), "인증 코드가 만료되었습니다."));
    }

    // 인증 성공
    List<String> ids = memberService.findAllIdsByEmail(dto.getEmail());
    if (ids == null) ids = Collections.emptyList();

    // 인증 성공 후 세션 삭제
    session.removeAttribute("findMidCode");
    session.removeAttribute("findMidEmail");
    session.removeAttribute("findMidCodeTime");

    return ResponseEntity.ok(new FindMidResponseDto(ids, "인증 성공"));
  }

  // 회원정보 수정 폼
  @GetMapping("/memberUpdate")
  public String memberUpdateGet(Model model, Principal principal) {

    String email = principal.getName(); // 로그인한 사용자의 이메일

    Member member = memberService.getMemberByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

    MemberUpdateDto dto = MemberUpdateDto.entityToDto(Optional.of(member));
    model.addAttribute("member", dto);

    return "member/memberUpdate";
  }

  // 회원정보 수정 처리
  @PostMapping("/memberUpdate")
  public String memberUpdatePost(@Valid @ModelAttribute("member") MemberUpdateDto dto,
                                 BindingResult bindingResult,
                                 Principal principal,
                                 RedirectAttributes rttr,
                                 HttpSession session) {
    // 유효성 검사 오류
    if (bindingResult.hasErrors()) {
      bindingResult.getAllErrors().forEach(System.out::println);
      return "member/memberUpdate";
    }

    try {
      // mid 기준으로 회원 조회
      Member member = memberService.getMemberByMid(dto.getMid())
              .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

      // 로그인 사용자 확인: mid와 principal.getName() 매칭 확인
      if (!principal.getName().equals(member.getEmail()) && !principal.getName().equals(member.getMid())) {
        throw new AccessDeniedException("잘못된 접근입니다.");
      }

      // 이메일 중복/인증 체크
      String originalEmail = member.getEmail();
      String newEmail = dto.getEmail1() + "@" + dto.getEmail2();
      boolean emailChanged = !originalEmail.equalsIgnoreCase(newEmail);

      if (emailChanged) {
        Optional<Member> existing = memberService.getMemberByEmail(newEmail);

        if (existing.isPresent() && !existing.get().getEmail().equalsIgnoreCase(originalEmail)) {
          rttr.addFlashAttribute("message", "이미 사용중인 이메일입니다.");
          return "redirect:/member/memberUpdate";
        }
        if (!dto.isEmailVerified()) {
          throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }
      }

      // DTO → 엔티티 업데이트
      member.setName(dto.getName());
      member.setEmail(newEmail);
      member.setTel(dto.getTel1() + "-" + dto.getTel2() + "-" + dto.getTel3());
      member.setAddress(String.join("/", dto.getPostcode(), dto.getRoadAddress(), dto.getDetailAddress()));
      member.setGender(dto.getGender());
      member.setBirthday(dto.getBirthday());
      member.setContent(dto.getContent());

      // 삭제 여부 판단
      boolean deleteSw = "1".equals(dto.getDeleteImageHidden());

      // 회원 정보 + 파일 업데이트
      memberService.updateMember(member, dto.getFile(), deleteSw);

      // 이메일 변경 시 로그아웃 처리
      if (emailChanged) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        rttr.addFlashAttribute("message", "회원정보가 수정되었습니다.\n이메일 변경으로 인해 다시 로그인해야 합니다.");
        return "redirect:/member/memberLogin";
      }

      // 세션 갱신
      session.setAttribute("loginMember", member);

      rttr.addFlashAttribute("message", "회원정보가 수정되었습니다.");
      return "redirect:/member/memberMain";
    } catch (Exception e) {
      rttr.addFlashAttribute("message", "회원정보 수정 실패: " + e.getMessage());
      return "member/memberUpdate";
    }
  }

  // 회원탈퇴
  @PostMapping("/memberDelete")
  public String memberDeleteAction(Principal principal, HttpServletRequest request, HttpServletResponse response, Model model) {
    String email = principal.getName();
    memberService.getMemberDelete(email);

    // 로그아웃 처리
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      new SecurityContextLogoutHandler().logout(request, response, auth);
    }

    // 메시지 + 리다이렉트 URL
    model.addAttribute("message", "정상적으로 탈퇴완료 되었습니다.");
    model.addAttribute("redirectUrl", "/springGroupBA/");

    return "include/message";
  }
}
