package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.member.MemberRepository;
import com.example.springGroupBA.repository.member.MessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMemberService {
  private final MemberRepository memberRepository;
  private final MessageRepository messageRepository;

  public Page<Member> searchMembers(String search, String keyword, Pageable pageable, boolean onlyDeleted) {
    UserDel del = onlyDeleted ? UserDel.OK : null;

    if (keyword == null || keyword.isEmpty()) {
      return onlyDeleted
              ? memberRepository.findByUserDel(UserDel.OK, pageable)
              : memberRepository.findAll(pageable);
    }

    switch (search) {
      case "mid":
        return onlyDeleted
                ? memberRepository.findByMidContainingAndUserDel(keyword, UserDel.OK, pageable)
                : memberRepository.findByMidContainingAndUserDel(keyword, UserDel.NO, pageable); // 일반회원
      case "name":
        return onlyDeleted
                ? memberRepository.findByNameContainingAndUserDel(keyword, UserDel.OK, pageable)
                : memberRepository.findByNameContainingAndUserDel(keyword, UserDel.NO, pageable);
      case "email":
        return onlyDeleted
                ? memberRepository.findByEmailContainingAndUserDel(keyword, UserDel.OK, pageable)
                : memberRepository.findByEmailContainingAndUserDel(keyword, UserDel.NO, pageable);
      default:
        return onlyDeleted
                ? memberRepository.findByUserDel(UserDel.OK, pageable)
                : memberRepository.findAll(pageable);
    }
  }

  // 토글
  public boolean getToggleMemberDel(Long id) {
    Optional<Member> memberOpt = memberRepository.findById(id);
    if (memberOpt.isEmpty()) return false;

    Member member = memberOpt.get();
    member.setUserDel(member.getUserDel() == UserDel.NO ? UserDel.OK : UserDel.NO);
    memberRepository.save(member);

    return true;
  }

  public Optional<Member> getMemberById(Long id) {
    return memberRepository.findById(id);
  }

  @Transactional
  public boolean getMemberDelete(Long id) {
    Optional<Member> memberOpt = memberRepository.findById(id);
    if (memberOpt.isEmpty()) return false;

    Member member = memberOpt.get();

    // 연관 메시지 먼저 삭제
    messageRepository.deleteByMemberSendId(member);
    messageRepository.deleteByMemberReceiveId(member);

    memberRepository.delete(member);
    return true;
  }

  public void getGradeUpdate(Member member) {
    memberRepository.save(member);
  }
}
