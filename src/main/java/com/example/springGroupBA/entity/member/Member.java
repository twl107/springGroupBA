package com.example.springGroupBA.entity.member;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.member.MemberDto;
import com.example.springGroupBA.entity.gallery.Gallery;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.io.FilenameUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_id", nullable = false)
  private Long id;

  @Column(length = 10)
  private String name;

  @Column(unique = true, length = 40, nullable = false)
  private String mid;

  @Column(unique = true, length = 20, nullable = false)
  private String nickName;

  @Column(unique = true, length = 50, nullable = false)
  private String email;

  @Column(nullable = false, length = 100)
  private String password;

  @Column(length = 20)
  private String tel;

  @Column(length = 255)
  private String address;

  @Column(nullable = false, length = 10)
  private String gender;

  private String birthday;

  @Column(length = 500)
  private String content;

  private String photoName;

  @Column(nullable = false)
  private LocalDateTime joinDate;

  @Enumerated(EnumType.STRING)
  private Role role;

  @Enumerated(EnumType.STRING)
  private UserDel userDel;

  @Column
  private LocalDateTime deleteRequestDate; // 탈퇴 요청일

  @Column
  private LocalDateTime deleteDate;   // 탈퇴 완료일

  @Column(length = 100)
  private String resetToken; // 비밀번호 재설정
  private LocalDateTime resetTokenExpiration;

  @PrePersist
  public void prePersist() {
    if (this.joinDate == null) {
      this.joinDate = LocalDateTime.now();
    }
  }

  // DTO → Entity
  public static Member dtoToEntity(MemberDto dto, PasswordEncoder passwordEncoder) {
    // 이메일 합치기
    String email = dto.getEmail1() + "@" + dto.getEmail2();

    // 주소 합치기
    String address = String.join("/",
            dto.getPostcode(),
            dto.getRoadAddress(),
            dto.getDetailAddress()
    );

    // 전화번호 합치기
    String tel = dto.getTel1() + "-" + dto.getTel2() + "-" + dto.getTel3();

    return Member.builder()
            .name(dto.getName())
            .mid(dto.getMid())
            .nickName(dto.getNickName())
            .email(email)
            .password(passwordEncoder.encode(dto.getPassword()))
            .tel(tel)
            .address(address)
            .gender(dto.getGender())
            .birthday(dto.getBirthday() != null ? dto.getBirthday().toString() : null)
            .content(dto.getContent())
            .photoName((dto.getFile() != null
                    && !dto.getFile().isEmpty()
                    && dto.getFile().getOriginalFilename() != null
                    && !dto.getFile().getOriginalFilename().trim().isEmpty())
                    ? UUID.randomUUID() + "." + FilenameUtils.getExtension(dto.getFile().getOriginalFilename())
                    : "noimage.jpg")
            .role(Role.USER)
            .userDel(UserDel.NO)
            .build();
  }
}

