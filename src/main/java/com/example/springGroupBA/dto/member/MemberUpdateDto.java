package com.example.springGroupBA.dto.member;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.entity.member.Member;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberUpdateDto {

  private Long id;

  // 이름
  @NotEmpty(message = "이름은 필수입력 입니다.")
  @Length(min = 1, max = 20, message = "1~20자 이하로 입력해 주세요.")
  private String name;

  // 아이디(mid)
  @NotEmpty(message = "아이디는 필수입력 입니다.")
  @Length(min = 4, max = 20, message = "4~20자 이하로 입력해 주세요.")
  private String mid;

  // 닉네임
  @NotEmpty(message = "닉네임은 필수입력 입니다.")
  @Length(min = 2, max = 20, message = "2~20자 이하로 입력해 주세요.")
  private String nickName;

  // 이메일 (앞/뒤)
  @Length(message = "이메일 형식을 맞춰주세요")
  private String email1;

  @Length(message = "이메일 형식을 맞춰주세요")
  private String email2;

  // 이메일 인증 여부
  @Builder.Default
  private boolean emailVerified = false;

  // 전화번호
  @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식을 확인하세요.")
  private String tel;

  @NotEmpty(message = "전화번호는 필수입력 입니다.")
  private String tel1;

  @NotEmpty(message = "전화번호는 필수입력 입니다.")
  private String tel2;

  @NotEmpty(message = "전화번호는 필수입니다.")
  private String tel3;

  // 주소
  private String address;
  private String postcode;
  private String roadAddress;
  private String detailAddress;

  // 성별
  private String gender;

  // 생년월일
  private String birthday;

  // 자기소개
  private String content;

  // 프로필 이미지
  private MultipartFile file;
  private String photoName;

  @Builder.Default
  private String deleteImageHidden = "0";

  private Role role;

  public String getEmail() {
    String local = email1 != null ? email1 : "";
    String domain = email2 != null ? email2 : "";
    return local + "@" + domain;
  }

  // Entity → DTO 변환
  public static MemberUpdateDto entityToDto(Optional<Member> opMember) {

    Member m = opMember.orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

    // 이메일 분리
    String email1 = null, email2 = null;
    if (m.getEmail() != null && m.getEmail().contains("@")) {
      String[] split = m.getEmail().split("@");
      email1 = split[0];
      email2 = split[1];
    }

    // 전화번호 분리
    String tel1 = null, tel2 = null, tel3 = null;
    if (m.getTel() != null && m.getTel().contains("-")) {
      String[] split = m.getTel().split("-");
      tel1 = split[0];
      tel2 = split[1];
      tel3 = split[2];
    }

    // 주소 분리
    String postcode = "", road = "", detail = "";
    if (m.getAddress() != null && m.getAddress().contains("/")) {
      String[] split = m.getAddress().split("/", 3);
      postcode = split.length > 0 ? split[0] : "";
      road = split.length > 1 ? split[1] : "";
      detail = split.length > 2 ? split[2] : "";
    }

    String address = String.join("/", postcode, road, detail);

    return MemberUpdateDto.builder()
            .id(m.getId())
            .name(m.getName())
            .mid(m.getMid())
            .nickName(m.getNickName())
            .email1(email1)
            .email2(email2)
            .tel1(tel1)
            .tel2(tel2)
            .tel3(tel3)
            .tel(tel1 + "-" + tel2 + "-" + tel3)
            .postcode(postcode)
            .roadAddress(road)
            .detailAddress(detail)
            .address(address)
            .gender(m.getGender())
            .birthday(m.getBirthday())
            .content(m.getContent())
            .photoName(m.getPhotoName())
            .role(m.getRole())
            .build();
  }
}
