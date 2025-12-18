package com.example.springGroupBA.entity.member;

import com.example.springGroupBA.dto.member.MessageDto;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "message_id")
  private Long id;

  @Column(length = 100, nullable = false)
  private String title;

  @Lob
  @NotNull
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="sendId", referencedColumnName = "email")
  private Member memberSendId;

  @Column(length = 1)
  @ColumnDefault("'s'")
  private String sendSw;

  private LocalDateTime sendDate;   // 보낸 시간

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="receiveId", referencedColumnName = "email")
  private Member memberReceiveId;

  @Column(length = 1)
  @ColumnDefault("'n'")
  private String receiveSw;

  private LocalDateTime receiveDate;  // 받은 시간(처음엔 null)

  private LocalDateTime readDate;    // 읽은 시간 (수신확인용)

  @ColumnDefault("1")
  private int msgSw;

  // 메세지 저장 시 자동 값 설정
  @PrePersist
  public void prePersist() {
    if (this.sendDate == null)
      this.sendDate = LocalDateTime.now();

    if (this.receiveSw == null)
      this.receiveSw = "n";

    if (this.sendSw == null)
      this.sendSw = "s";

    if (this.msgSw == 0)
      this.msgSw = 1;
  }

  // DTO → Entity (날짜는 절대로 DTO에서 가져오면 안 됨)
  public static Message dtoToEntity(MessageDto dto, Member sender, Member receiver) {
    return Message.builder()
            .title(dto.getTitle())
            .content(dto.getContent())
            .memberSendId(sender)
            .sendSw("s")
            .memberReceiveId(receiver)
            .receiveSw("n")
            .msgSw(1)
            .build();
  }
}
