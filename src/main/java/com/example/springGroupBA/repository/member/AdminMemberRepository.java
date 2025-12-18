package com.example.springGroupBA.repository.member;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.entity.member.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminMemberRepository extends JpaRepository<Member, Long> {

  // 회원 검색
  Page<Member> findByMidContaining(String mid, Pageable pageable);
  Page<Member> findByNameContaining(String name, Pageable pageable);
  Page<Member> findByEmailContaining(String email, Pageable pageable);

  Page<Member> findByMidContainingAndRole(String mid, Role role, Pageable pageable);
  Page<Member> findByNameContainingAndRole(String name, Role role, Pageable pageable);
  Page<Member> findByEmailContainingAndRole(String email, Role role, Pageable pageable);

  Page<Member> findByRole(Role role, Pageable pageable);

  Page<Member> findByUserDel(UserDel userDel, PageRequest pageable);

  Page<Member> findByUserDelAndNickNameContaining(UserDel userDel, String nickName, PageRequest pageable);

}
