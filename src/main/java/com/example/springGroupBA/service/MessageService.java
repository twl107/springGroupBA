package com.example.springGroupBA.service;

import com.example.springGroupBA.entity.member.Message;
import com.example.springGroupBA.repository.member.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class MessageService {
  private final MessageRepository webMessageRepository;

  public Message readMessage(Long id, String mid) {
    Message webMessage = webMessageRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("메시지를 찾을 수 없음"));

    // 받은 사람만 읽음 처리
    if ("n".equals(webMessage.getReceiveSw()) &&
            webMessage.getMemberReceiveId() != null &&
            webMessage.getMemberReceiveId().getEmail().equals(mid)) {
      webMessage.setReceiveSw("r");
      webMessage.setReadDate(LocalDateTime.now());

      if (webMessage.getReceiveDate() == null) {
        webMessage.setReceiveDate(LocalDateTime.now());
      }

      webMessageRepository.save(webMessage);
    }

    return webMessage;
  }

  public Optional<Message> findById(Long id) {
    return webMessageRepository.findById(id);
  }

  @Transactional
  public void deleteMessage(Long id) {
    webMessageRepository.markAsDeletedById(id);
  }
}
