package com.example.springGroupBA.service;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.constant.UserDel;
import com.example.springGroupBA.dto.member.KakaoDto;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.repository.member.MemberRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class KakaoService {

  private final MemberRepository memberRepository;

  @Value("${kakao.client.id}")
  private String KAKAO_CLIENT_ID;

  @Value("${kakao.client.secret}")
  private String KAKAO_CLIENT_SECRET;

  @Value("${kakao.redirect.url}")
  private String KAKAO_REDIRECT_URL;

  private static final String KAKAO_AUTH_URI = "https://kauth.kakao.com";
  private static final String KAKAO_API_URI = "https://kapi.kakao.com";

  public String getKakaoLogin() {
    return KAKAO_AUTH_URI + "/oauth/authorize"
            + "?client_id=" + KAKAO_CLIENT_ID
            + "&redirect_uri=" + KAKAO_REDIRECT_URL
            + "&response_type=code"
            + "&prompt=login";
  }

  public KakaoDto getKakaoInfo(String code, HttpSession session) throws Exception {
    if(code == null) throw new IllegalArgumentException("Authorization code is null");

    String accessToken = (String) session.getAttribute("kakaoAccessToken");
    if(accessToken == null) {
      accessToken = getAccessToken(code);
      session.setAttribute("kakaoAccessToken", accessToken);
    }

    return getUserInfoWithToken(accessToken, session);
  }

  private String getAccessToken(String code) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", KAKAO_CLIENT_ID);
    params.add("client_secret", KAKAO_CLIENT_SECRET);
    params.add("redirect_uri", KAKAO_REDIRECT_URL);
    params.add("code", code);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
    RestTemplate rt = new RestTemplate();
    ResponseEntity<String> response = rt.exchange(KAKAO_AUTH_URI + "/oauth/token", HttpMethod.POST, request, String.class);

    JSONObject json = (JSONObject) new JSONParser().parse(response.getBody());
    return (String) json.get("access_token");
  }

  private KakaoDto getUserInfoWithToken(String accessToken, HttpSession session) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + accessToken);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    RestTemplate rt = new RestTemplate();
    HttpEntity<?> entity = new HttpEntity<>(headers);
    ResponseEntity<String> response = rt.exchange(KAKAO_API_URI + "/v2/user/me", HttpMethod.GET, entity, String.class);

    JSONObject json = (JSONObject) new JSONParser().parse(response.getBody());
    JSONObject account = (JSONObject) json.get("kakao_account");
    JSONObject profile = account != null ? (JSONObject) account.get("profile") : null;

    Long id = (Long) json.get("id");
    String email = account != null ? (String) account.get("email") : null;
    String nickname = profile != null ? (String) profile.get("nickname") : "카카오유저";

    Member member = (email != null) ? memberRepository.findByEmail(email).orElse(null) : null;

    if(member == null) {
      String emailPrefix = (email != null && !email.isEmpty()) ? email.split("@")[0] : "user";
      String midValue = emailPrefix + "_kakao";

      member = Member.builder()
              .name(nickname)
              .mid(midValue)
              .nickName(emailPrefix)
              .password("")
              .email(email != null ? email : "정보 없음")
              .tel("정보 없음")
              .birthday("정보 없음")
              .address("정보 없음")
              .gender("정보 없음")
              .joinDate(LocalDateTime.now())
              .role(Role.USER)
              .photoName("noimage.jpg")
              .userDel(UserDel.NO)
              .build();

      member = memberRepository.save(member);
    }

    // SecurityContext 설정
    String username = email != null ? email : "kakao_" + id;
    UserDetails userDetails = User.builder()
            .username(username)
            .password("")
            .roles("USER")
            .build();

    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);

    session.setAttribute("loginMember", member);
    session.setAttribute("loginType", "kakao");
    session.setAttribute("kakaoNickName", nickname);
    session.setAttribute(
            org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            SecurityContextHolder.getContext()
    );

    return KakaoDto.builder().id(id).email(email).nickName(nickname).build();
  }

  public void kakaoLogout(HttpSession session) {
    if(session == null) return;

    String accessToken = (String) session.getAttribute("kakaoAccessToken");
    if(accessToken == null) return;

    try {
      RestTemplate rt = new RestTemplate();
      HttpHeaders headers = new HttpHeaders();
      headers.add("Authorization", "Bearer " + accessToken);

      HttpEntity<?> entity = new HttpEntity<>(headers);
      rt.exchange("https://kapi.kakao.com/v1/user/logout", HttpMethod.POST, entity, String.class);
    } catch(Exception e) {
      throw new RuntimeException("카카오 로그아웃 실패", e);
    } finally {
      session.removeAttribute("kakaoAccessToken");
      session.removeAttribute("kakaoEmail");
      session.removeAttribute("kakaoNickName");
    }
  }
}
