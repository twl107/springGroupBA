package com.example.springGroupBA.service;

import com.example.springGroupBA.common.ProjectProvide;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.member.FindMidRequestDto;
import com.example.springGroupBA.dto.member.FindMidResponseDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.member.MemberRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

  private final MemberRepository memberRepository;
  private final FileUploadService fileUploadService;
  private final PasswordEncoder passwordEncoder;
  private final ProjectProvide projectProvide;

  // 업로드 경로
  private final String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/member";

  // 회원 가입
  public Member saveMember(Member member) {
    if (memberRepository.existsByMid(member.getMid())) {
      throw new IllegalStateException("이미 존재하는 아이디입니다.");
    }

    if (memberRepository.existsByNickName(member.getNickName())) {
      throw new IllegalStateException("이미 존재하는 닉네임입니다.");
    }

    return memberRepository.save(member);
  }

  // 비밀번호 변경
  @Transactional
  public boolean changePassword(String email, String newPwd) {
    Optional<Member> optMember = memberRepository.findByEmail(email);
    if (optMember.isEmpty()) return false;

    Member member = optMember.get();
    member.setPassword(passwordEncoder.encode(newPwd));
    // memberRepository.save(member);

    return true;
  }

  // 비밀번호 재설정 요청
  public void requestPasswordReset(String mid, String email) throws MessagingException {
    Member member = memberRepository.findByMidAndEmail(mid, email)
            .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

    // 30분 유효 토큰 생성
    String token = UUID.randomUUID().toString();
    member.setResetToken(token);
    member.setResetTokenExpiration(LocalDateTime.now().plusMinutes(30));
    memberRepository.save(member);

    // 서버 URL 동적 생성
    String resetLink = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/member/resetPwd")
            .queryParam("token", token)
            .toUriString();

    String mailContent = "<p>아래 링크를 클릭하여 비밀번호를 재설정하세요. (30분 유효)</p>"
            + "<a href='" + resetLink + "'>" + resetLink + "</a>";

    projectProvide.mailSend(member.getEmail(), "SpringBootBA 비밀번호 재설정", mailContent);
  }

  // 토큰 유효성 검사
  public boolean isResetTokenValid(String token) {
    return memberRepository.findByResetToken(token)
            .filter(m -> m.getResetTokenExpiration().isAfter(LocalDateTime.now()))
            .isPresent();
  }

  // 비밀번호 재설정
  public void resetPassword(String token, String newPassword) {
    Member member = memberRepository.findByResetToken(token)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰입니다."));

    if (member.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("토큰이 만료되었습니다.");
    }

    // 비밀번호 체크
    if (newPassword.length() < 4 || newPassword.length() > 20) {
      throw new IllegalArgumentException("비밀번호는 4~20자리로 입력하세요.");
    }

    member.setPassword(passwordEncoder.encode(newPassword));
    member.setResetToken(null);
    member.setResetTokenExpiration(null);
    memberRepository.save(member);
  }

  // 아이디 찾기
  public Map<String, String> findIds(String email, String tel) {
    String numTel = tel.replaceAll("[^0-9]", "");

    Member member = memberRepository.findByEmail(email)
            .filter(m -> m.getTel() != null && m.getTel().replaceAll("[^0-9]", "").equals(numTel))
            .orElse(null);

    if (member == null) {
      return Map.of();
    }

    return Map.of(
            ProjectProvide.maskId(member.getMid()),
            member.getEmail()
    );
  }

  // 아이디 전체 찾기
  public List<String> findAllIdsByEmail(String email) {
    return  memberRepository.findByEmail(email)
            .map(member -> List.of(member.getMid()))
            .orElse(Collections.emptyList());
  }

  // 회원 수정
  @Transactional
  public Member joinMember(Member member, MultipartFile file) throws IOException {
    String fileName = processFile(member, file, false);
    member.setPhotoName(fileName);

    return memberRepository.save(member);
  }

  @Transactional
  public Member updateMember(Member member, MultipartFile file, boolean deleteSw) throws IOException {
    String newFileName = processFile(member, file, deleteSw);
    member.setPhotoName(newFileName);

    return memberRepository.save(member);
  }

  private String processFile(Member member, MultipartFile file, boolean deleteSw) throws IOException {
    // 파일이 없을 때
    if (file == null || file.isEmpty() ||
            file.getOriginalFilename() == null || file.getOriginalFilename().trim().isEmpty()) {

      // 삭제만 요청
      if (deleteSw) {
        String oldFile = member.getPhotoName();
        if (oldFile != null && !oldFile.equals("noimage.jpg")) {
          fileUploadService.deleteFile(oldFile, "member");
        }
        return "noimage.jpg";
      }

      // 기존 파일 그대로
      String photo = member.getPhotoName();
      if (photo == null || photo.trim().isEmpty() || photo.endsWith(".")) {
        return "noimage.jpg";
      }
      return photo;
    }

    // 새 파일 업로드 시 기존 파일 삭제
    if (deleteSw) {
      String oldFile = member.getPhotoName();
      if (oldFile != null && !oldFile.equals("noimage.jpg")) {
        fileUploadService.deleteFile(oldFile, "member");
      }
    }

    return fileUploadService.saveFile(file, member.getMid(), "member");
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("회원 정보가 없습니다: " + email));

    return org.springframework.security.core.userdetails.User.builder()
            .username(member.getEmail())
            .password(member.getPassword())
            .roles(member.getRole().name())
            .disabled(member.getUserDel() == UserDel.OK)
            .build();
  }

  public Page<Member> getMemberSearch(String keyword, Pageable pageable) {
    if (keyword == null || keyword.isEmpty()) {
      return memberRepository.findAll(pageable);
    }
    return memberRepository.findByNameContainingOrEmailContaining(keyword, keyword, pageable);
  }

  public void getMemberDelete(String email) {
    Member member = memberRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 회원입니다."));

    member.setUserDel(UserDel.OK);

    // 탈퇴 요청일
    if (member.getDeleteRequestDate() == null) {
      member.setDeleteRequestDate(LocalDateTime.now());
    }

    memberRepository.save(member);
  }

  public boolean getMemberIdCheck(String mid) {
    return memberRepository.existsByMid(mid);
  }

  public boolean getMemberNickNameCheck(String nickName) {
    return memberRepository.existsByNickName(nickName);
  }

  public Optional<Member> getMemberByEmail(String email) {
    return memberRepository.findByEmail(email);
  }

  public Optional<Member> getMemberByMid(String mid) {
    return memberRepository.findByMid(mid);
  }

  public void validateJoinByMid(String mid) {
    Optional<Member> optMember = memberRepository.findByMid(mid);

    if (optMember.isEmpty()) {
      // 아이디가 존재하지 않으면 가입 가능
      return;
    }

    Member member = optMember.get();

    if (member.getUserDel() == UserDel.OK) {
      // 탈퇴 회원이면 삭제일 기준 7일 제한 체크
      if (member.getDeleteDate() != null) {
        LocalDateTime limitDate = member.getDeleteDate().plusDays(7);
        if (LocalDateTime.now().isBefore(limitDate)) {
          throw new IllegalArgumentException(
                  "해당 아이디는 탈퇴처리된 아이디입니다. 7일 동안 재가입할 수 없습니다."
          );
        }
      }
      // 탈퇴 후 7일이 지난 경우, 가입 가능
    } else {
      // 탈퇴 상태가 아니면 이미 사용 중
      throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
    }
  }
}
