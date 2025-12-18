package com.example.springGroupBA.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Value("${ncp.client-id}")
    private String ncpKeyId;

    @GetMapping("/include/message")
    public String message() {
        return "include/message";
    }

    @GetMapping("/")
    public String homeGet() {
        return "main/index";
    }

    @GetMapping("/content/business")
    public String businessGet() {
        return "content/business";
    }

    @GetMapping("/content/technology")
    public String technologyGet() {
        return "content/technology";
    }

    @GetMapping("/content/solution")
    public String solutionGet() {
        return "content/solution";
    }

    @GetMapping("/content/clients")
    public String clientsGet() {
        return "content/clients";
    }

    @GetMapping("/content/company")
    public String companyGet() {
        return "content/company";
    }

    @GetMapping("/content/contact")
    public String contactGet(Model model) {
        model.addAttribute("ncpKeyId", ncpKeyId);
        return "content/contact";
    }

  // FAQ
  @GetMapping("/faq")
  public String faqGet(Model model) {
    List<Map<String, String>> faqList = List.of(
            Map.of("category", "회원/로그인", "question", "회원가입은 어떻게 하나요?", "answer", "로그인 페이지에서 회원가입 버튼을 눌러 필요한 정보를 입력하면 가입이 완료됩니다."),
            Map.of("category", "회원/로그인", "question", "아이디를 잊어버렸어요.", "answer", "로그인 페이지에서 아이디 찾기 기능을 이용하세요."),
            Map.of("category", "회원/로그인", "question", "비밀번호를 잊어버렸어요.", "answer", "로그인 페이지에서 비밀번호 찾기 기능을 이용하세요."),
            Map.of("category", "회원/로그인", "question", "비밀번호를 변경하고 싶어요.", "answer", "마이페이지에서 비밀번호 변경을 이용하세요."),

            Map.of("category", "계정 설정", "question", "개인정보를 변경하고 싶어요.", "answer", "마이페이지에서 회원정보 수정을 이용하세요."),
            Map.of("category", "계정 설정", "question", "회원 탈퇴는 어떻게 하나요?", "answer", "마이페이지에서 회원탈퇴 가능합니다."),
            Map.of("category", "계정 설정", "question", "프로필 이미지는 어떻게 변경하나요?", "answer", "마이페이지 > 회원정보 수정에서 프로필 이미지를 업로드하여 변경할 수 있습니다."),

            Map.of("category", "서비스 이용", "question", "이메일 인증이 오지 않아요.", "answer", "스팸메일함을 확인하거나, 이메일 주소를 다시 확인해주세요."),
            Map.of("category", "서비스 이용", "question", "로그인 시도가 여러 번 실패합니다.", "answer", "비밀번호를 재설정하거나 관리자에게 문의하세요."),
            Map.of("category", "서비스 이용", "question", "게시글 작성은 어떻게 하나요?", "answer", "로그인 후 게시판에서 글쓰기 버튼을 누르면 작성할 수 있습니다."),
            Map.of("category", "서비스 이용", "question", "메세지를 어디서 확인하나요?", "answer", "로그인 후 마이페이지에서 최근 메세지를 확인할수 있고, 웹 메세지에서 메세지를 보낼 수 있습니다.")
    );

    // 카테고리
    Map<String, List<Map<String, String>>> groupedFaq =
            faqList.stream().collect(Collectors.groupingBy(f -> f.get("category")));

    model.addAttribute("groupedFaq", groupedFaq);

    return "faq/faq";
  }

}
