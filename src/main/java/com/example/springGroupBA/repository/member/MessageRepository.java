package com.example.springGroupBA.repository.member;

import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.entity.member.Message;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

  List<Message> findByMemberReceiveId_EmailAndReceiveSw(String mid, String receiveSw);


  @Modifying      // 이 쿼리가 데이터를 변경(삭제/수정)함을 JPA에 알림
  @Transactional  // 이 메서드 실행 시 트랜잭션을 시작하도록 함
  void deleteByReceiveSwAndSendSw(String receiveSw, String sendSw);

  // 전체 메시지 (검색 없음)
  @Query("SELECT m FROM Message m ORDER BY m.id DESC")
  Page<Message> findAllMessages(Pageable pageable);

  // 전체 메시지 (검색)
  @Query("""
  SELECT m FROM Message m
  WHERE
    (:search = 'title' AND m.title LIKE %:keyword%)
    OR
    (:search = 'senderName' AND m.memberSendId.email LIKE %:keyword%)
    OR
    (:search = 'receiverName' AND m.memberReceiveId.email LIKE %:keyword%)
  ORDER BY m.id DESC
""")
  Page<Message> searchAllMessages(
          @Param("search") String search,
          @Param("keyword") String keyword,
          Pageable pageable
  );

  @Query("SELECT w FROM Message w WHERE w.memberReceiveId.email = :mid AND (w.receiveSw = 'n' OR w.receiveSw = 'r') ORDER BY w.id DESC")
  Page<Message> findReceivedMessages(@Param("mid") String mid, Pageable pageable);

  @Query("SELECT w FROM Message w WHERE w.memberReceiveId.email = :mid AND (w.receiveSw = 'n') ORDER BY w.id DESC")
  Page<Message> findNewMessages(@Param("mid") String mid, Pageable pageable);

  @Query("SELECT w FROM Message w WHERE w.memberSendId.email = :mid AND (w.sendSw='s') ORDER BY w.id DESC")
  Page<Message> findSendMessages(@Param("mid") String mid, Pageable pageable);

  @Query("SELECT w FROM Message w WHERE w.memberSendId.email = :mid AND (w.receiveSw='n') ORDER BY w.id DESC")
  Page<Message> findReceiveCheckMessages(@Param("mid") String mid, Pageable pageable);

  @Query("SELECT w FROM Message w WHERE (w.memberReceiveId.email = :mid AND w.receiveSw='g') OR (w.memberSendId.email = :mid AND w.sendSw='g') ORDER BY w.id DESC")
  Page<Message> findWasteBasketMessages(@Param("mid") String mid, Pageable pageable);

  @Query("SELECT m FROM Message m WHERE m.memberReceiveId.email = :email AND m.receiveSw IN :receiveSwList ORDER BY m.id DESC")
  List<Message> findByMessageRead(@Param("email") String email, @Param("receiveSwList") List<String> receiveSwList);

  long countByMemberReceiveId_EmailAndReceiveSw(String email, String receiveSw);

  // 새 메시지
  List<Message> findTop3ByMemberReceiveIdEmailAndReceiveSwAndReadDateIsNullOrderByReceiveDateDesc(String email, String receiveSw);

  // 받은 메시지
  List<Message> findTop3ByMemberReceiveIdEmailAndReceiveSwNotAndReadDateIsNotNullOrderByReadDateDesc(String email, String excludeSw);

  // 보낸 메시지
  List<Message> findTop3ByMemberSendIdEmailAndSendSwNotOrderBySendDateDesc(String email, String excludeSw);

  @Transactional
  void deleteByMemberSendId(Member member);

  @Transactional
  void deleteByMemberReceiveId(Member member);

  @Transactional
  @Modifying
  @Query("UPDATE Message m SET m.receiveSw = 'x' WHERE m.id = :id")
  void markAsDeletedById(@Param("id") Long id);
}
