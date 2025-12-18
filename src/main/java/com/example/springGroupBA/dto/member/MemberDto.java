package com.example.springGroupBA.dto.member;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.entity.member.Member;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {

  private Long id;

  // 이름
  @NotEmpty(message = "이름은 필수입력 입니다.")
  @Length(min = 1, max = 20, message = "1~20자 이하로 입력해 주세요.")
  private String name;

  // 아이디(mid)
  @NotEmpty(message = "아이디는 필수입력 입니다.")
  @Length(min = 4, max = 20, message = "4~20자 이하로 입력해 주세요.")
  private String mid;

  // 닉네임(nickName)
  @NotEmpty(message = "닉네임은 필수입력 입니다.")
  @Length(min = 2, max = 20, message = "2~20자 이하로 입력해 주세요.")
  private String nickName;

  // 이메일(앞/뒤 조합)
  @Length(message = "이메일 형식을 맞춰주세요")
  private String email1;

  @Length(message = "이메일 형식을 맞춰주세요")
  private String email2;

  // 비밀번호
  @NotEmpty(message = "비밀번호는 필수입력 입니다.")
  @Length(min = 4, max = 20, message = "4~20자 이하로 입력해 주세요.")
  private String password;

  @NotEmpty(message = "비밀번호는 필수입력 입니다.")
  @Length(min = 4, max = 20, message = "4~20자 이하로 입력해 주세요.")
  private String passwordCheck;

  // 전화번호
  @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식을 확인하세요.")
  private String tel;

  @NotEmpty(message = "전화번호는 필수입력 입니다.")
  @Length(message = "전화번호는 숫자로 입력해주세요")
  private String tel1;

  @NotEmpty(message = "전화번호는 필수입력 입니다.")
  @Length(message = "전화번호는 숫자로 입력해주세요")
  private String tel2;

  @NotEmpty(message = "전화번호는 필수입니다.")
  @Length(message = "전화번호는 숫자로 입력해주세요")
  private String tel3;

  // 주소 (4부분)
  private String address;
  private String postcode;      // 우편번호
  private String roadAddress;   // 기본주소
  private String detailAddress; // 상세주소

  // 성별
  private String gender;

  // 생년월일
  private String birthday;

  // 소개글
  private String content;

  // 프로필 이미지
  private MultipartFile file;
  private String photoName;

  private LocalDateTime joinDate;

  private Role role;


  //Entity → DTO 변환
  public static MemberDto entityToDto(Optional<Member> opMember) {

    Member m = opMember.orElseThrow(() -> new IllegalArgumentException("회원 정보가 없습니다."));

    /* -------- 이메일 split -------- */
    String email1 = null;
    String email2 = null;

    if (m.getEmail() != null && m.getEmail().contains("@")) {
      String[] emailSplit = m.getEmail().split("@");
      if (emailSplit.length == 2) {
        email1 = emailSplit[0];
        email2 = emailSplit[1];
      }
    }

    /* -------- 전화 split -------- */
    String tel1 = null, tel2 = null, tel3 = null;
    if (m.getTel() != null && m.getTel().contains("-")) {
      String[] telSplit = m.getTel().split("-");
      if (telSplit.length == 3) {
        tel1 = telSplit[0];
        tel2 = telSplit[1];
        tel3 = telSplit[2];
      }
    }

    /* -------- 주소 split -------- */
    String postcode = null, road = null, detail = null;
    if (m.getAddress() != null && m.getAddress().contains("/")) {
      String[] addrSplit = m.getAddress().split("/", 3);
      postcode = addrSplit.length > 0 ? addrSplit[0] : "";
      road = addrSplit.length > 1 ? addrSplit[1] : "";
      detail = addrSplit.length > 2 ? addrSplit[2] : "";
    }

    String address = String.join("/",
            postcode != null ? postcode : "",
            road != null ? road : "",
            detail != null ? detail : "");

    return MemberDto.builder()
            .id(m.getId())
            .name(m.getName())
            .mid(m.getMid())
            .nickName(m.getNickName())

            .email1(email1)
            .email2(email2)

            .password(m.getPassword())

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
            .joinDate(m.getJoinDate())
            .role(m.getRole())
            .build();
  }
}
