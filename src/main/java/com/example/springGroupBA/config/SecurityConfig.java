package com.example.springGroupBA.config;

import com.example.springGroupBA.constant.Role;
import com.example.springGroupBA.entity.member.Member;
import com.example.springGroupBA.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final MemberService memberService;

  public SecurityConfig(@Lazy MemberService memberService) {
    this.memberService = memberService;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

    /* ===== CSRF 설정 ===== */
    http.csrf(csrf -> csrf
            .ignoringRequestMatchers(
                    "/survey/**",
                    "/ckeditor/imageUpload",
                    "/api/**",
                    "/ws-sensor/**",
                    "/sensor/**",
                    "/order/payment/complete"
            )
            //.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    );

    /* ===== FORM LOGIN ===== */
    http.formLogin(form -> form
            .loginPage("/member/memberLogin")
            .loginProcessingUrl("/member/memberLogin")
            .defaultSuccessUrl("/member/memberMain", true)
            .failureUrl("/member/memberLogin?error=true")
            .successHandler(customSuccessHandler())
            .failureHandler(customFailureHandler())
            .usernameParameter("email")
            .passwordParameter("password")
            .permitAll()
    );

    /* ===== 세션 관리 ===== */
    http.sessionManagement(session -> session.sessionFixation().migrateSession());

    /* ===== 권한 설정 ===== */
    http.authorizeHttpRequests(request -> request

            /* ----- 정적 리소스 허용 ----- */
            .requestMatchers(
                    "/css/**", "/js/**", "/images/**",
                    "/upload/**", "/ckeditor/**", "/ckeditorUpload/**",
                    "/favicon.ico", "/include/**" , "/fireStat/**"
            ).permitAll()

            /* ----- 공용 페이지 허용 ----- */
            .requestMatchers(
                    "/", "/message/**", "/business",
                    "/guest/**",
                    "/content/**",
                    "/api/**",
                    "/pds/pdsList", "/pds/pdsContent", "/pds/download/**",
                    "/dbShop/**"
            ).permitAll()

            /* ----- 회원 관련 허용 ----- */
            .requestMatchers(
                    "/member/memberJoin", "/member/memberJoin/**",
                    "/member/memberLogin",
                    "/member/memberEmailCheck", "/member/memberEmailCheckOk",
                    "/member/memberEmailCheckNo",
                    "/member/idCheck", "/member/nickNameCheck",
                    "/member/memberLoginOk", "/member/memberLogout",
                    "/member/findMid", "/member/findMid/**",
                    "/member/findPwd", "/member/findPwd/**",
                    "/member/resetPwd", "/member/resetPwd/**",
                    "/member/kakaoLogin", "/member/kakaoLogout"
            ).permitAll()

            /* ----- 인증 필요 ----- */
            .requestMatchers(
                    "/member/memberMain",
                    "/pds/pdsInput", "/pds/pdsUpdate", "/pds/delete",
                    "/pds/vote/**", "/pds/reply/**", "/pds/report/**",
                    "/member/memberDelete",
                    "/member/memberPwdCheck", "/member/memberPwdCheck/**",
                    "/member/memberUpdate", "/member/memberUpdate/**",
                    "/survey/**"
            ).authenticated()

            /* ----- 관리자 ----- */
            .requestMatchers("/admin/**").hasRole("ADMIN")

            /* ----- SENSOR는 전체 허용 ----- */
            .requestMatchers("/sensor/**").permitAll()
            .requestMatchers("/ws-sensor/**").permitAll()
            .requestMatchers("/api/sensor/**").permitAll()

            /* ----- 그 외 ----- */
            .anyRequest().authenticated()
    );

    http.exceptionHandling(exception ->
            exception.accessDeniedPage("/error/accessDenied"));

    http.logout(Customizer.withDefaults());

    return http.build();
  }


  /* ===== 암호화 ===== */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  /* ===== 로그인 성공 처리 ===== */
  @Bean
  public AuthenticationSuccessHandler customSuccessHandler() {
    return (request, response, authentication) -> {

      HttpSession session = request.getSession();
      String email = authentication.getName();
      Member member = memberService.getMemberByEmail(email).orElseThrow();

      session.setAttribute("sName", member.getName());
      session.setAttribute("loginMember", member);
      session.setAttribute("strLevel", member.getRole().toString());

      boolean isAdmin = member.getRole() == Role.ADMIN;

      if (isAdmin) {
        session.setAttribute("message", "관리자 로그인 성공");
      } else {
        session.setAttribute("message", member.getName() + "님 로그인 성공");
      }

      response.sendRedirect("/springGroupBA/member/memberMain");
    };
  }

  /* ===== 로그인 실패 처리 ===== */
  @Bean
  public AuthenticationFailureHandler customFailureHandler() {
    return (request, response, exception) -> {

      String msg = "아이디 또는 비밀번호가 일치하지 않습니다.";

      if (exception instanceof DisabledException) {
        msg = "해당 계정은 탈퇴 처리중 입니다.\n탈퇴신청 날짜부터 7일 이후 재가입 가능합니다.";
      }

      request.getSession().setAttribute("message", msg);
      response.sendRedirect("/springGroupBA/member/memberLogin");
    };
  }
}
