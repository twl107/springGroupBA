package com.example.springGroupBA.repository.member;

import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.entity.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
  Optional<Member> findByEmail(String email);

  Optional<Member> findByMid(String mid);

  boolean existsByNickName(String nickName);

  boolean existsByMid(String mid);

  List<Member> findByUserDel(UserDel userDel);

  Optional<Member> findByMidAndEmail(String mid, String email);
  Optional<Member> findByResetToken(String resetToken);

  Page<Member> findByNameContainingOrEmailContaining(String name, String email, Pageable pageable);

  // 관리자 대시보드 추가
  long countByJoinDateBetween(LocalDateTime start, LocalDateTime end);

  // 탈퇴 회원 조회용
  Page<Member> findByUserDel(UserDel userDel, Pageable pageable);

  // 검색 + 탈퇴
  Page<Member> findByMidContainingAndUserDel(String mid, UserDel userDel, Pageable pageable);
  Page<Member> findByNameContainingAndUserDel(String name, UserDel userDel, Pageable pageable);
  Page<Member> findByEmailContainingAndUserDel(String email, UserDel userDel, Pageable pageable);
}