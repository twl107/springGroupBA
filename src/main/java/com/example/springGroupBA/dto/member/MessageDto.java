package com.example.springGroupBA.dto.member;

import com.example.springGroupBA.entity.member.Message;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDto {

  private Long id;

  @NotEmpty(message = "제목은 필수입력입니다.")
  @Column(length = 100, nullable = false)
  private String title;

  @NotEmpty(message = "메세지 내용은 필수입력입니다.")
  private String content;

  @NotEmpty(message = "보내는사람 아이디는 필수입력입니다.")
  @Column(length = 20, nullable = false)
  private String sendId;

  private String sendSw;

  private LocalDateTime sendDate;

  @NotEmpty(message = "받는사람 아이디는 필수입력입니다.")
  @Column(length = 20, nullable = false)
  private String receiveId;

  private String receiveSw;

  private LocalDateTime receiveDate;

  private LocalDateTime readDate;

  private int msgSw;

  public String getStatus() {
    if ("n".equals(receiveSw)) return "읽지않음";
    if ("r".equals(receiveSw)) return "읽음";
    if ("g".equals(receiveSw)) return "휴지통";
    if ("x".equals(receiveSw)) return "삭제";

    return "-";
  }

  public String getSendDateStr() {
    return sendDate != null ? sendDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
  }

  public String getReceiveDateStr() {
    return receiveDate != null ? receiveDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
  }

  public String getReadDateStr() {
    return readDate != null ? readDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-";
  }

  public static MessageDto entityToDto(Message message) {
    return MessageDto.builder()
            .id(message.getId())
            .title(message.getTitle())
            .content(message.getContent())
            .sendId(message.getMemberSendId() != null ? message.getMemberSendId().getEmail() : "-")
            .sendSw(message.getSendSw() != null ? message.getSendSw() : "s")
            .sendDate(message.getSendDate())
            .receiveId(message.getMemberReceiveId() != null ? message.getMemberReceiveId().getEmail() : "-")
            .receiveSw(message.getReceiveSw() != null ? message.getReceiveSw() : "n")
            .receiveDate(message.getReceiveDate())
            .readDate(message.getReadDate())
            .msgSw(message.getMsgSw())
            .build();
  }
}
