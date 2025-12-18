package com.example.springGroupBA.common;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

//@Component
@Service
public class ProjectProvide {

  @Value("org.zerock.upload.path")
  private String uploadFolder;

  @Autowired
  JavaMailSender mailSender;
  private String mailFlag;

  public String mailSend(String toMail, String title, String code) throws MessagingException {
    // HTML 템플릿 불러오기
    String content = loadHtmlTemplate("templates/member/mail/mailSend.html");

    // {{CODE}} 부분을 실제 인증코드로 치환
    content = content.replace("{{CODE}}", code);

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setTo(toMail);
    helper.setSubject(title);
    helper.setText(content, true);

    // CID 이미지 첨부
    ClassPathResource resource = new ClassPathResource("static/images/main/logo_greensoft.png");
    helper.addInline("logo_greensoft.png", resource);

    // 메일 전송
    mailSender.send(message);
    return "1";
  }

  // html Template
  private String loadHtmlTemplate(String path) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
      if (inputStream == null) {
        throw new FileNotFoundException("템플릿을 찾을 수 없습니다: " + path);
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("템플릿 로딩 실패: " + path, e);
    }
  }



  private String sendMailCore(String toMail, String title, String content) throws MessagingException {

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

    helper.setTo(toMail);
    helper.setSubject(title);
    helper.setText(content, true);

    ClassPathResource resource = new ClassPathResource("static/images/main/logo_greensoft.png");
    helper.addInline("logo_greensoft.png", resource);

    mailSender.send(message);

    return "OK";
  }

  public String sendApplyMail(String toMail, String name, String position) throws MessagingException {

    String title = "[GreenSoft] 지원 접수 안내";

    String content = "";
    content += "<h3>" + name + "님, 지원이 접수되었습니다.</h3>";
    content += "<hr>";
    content += "<p>지원 직무 : <b>" + position + "</b></p>";
    content += "<p>지원해주셔서 감사합니다.</p>";
    content += "<p>검토 후 연락드리겠습니다.</p>";
    content += "<br>";
    content += "<p><img src=\"cid:logo_greensoft.png\" width='450px'></p>";

    // mailSend 재사용
    return sendMailCore(toMail, title, content);
  }

  // 파일 업로드 후 서버에 구분처리해서 저장하기
  public String fileUpload(MultipartFile fName, String mid, String part) {
    // 파일중복처리
    String oFileName = fName.getOriginalFilename();
    String sFileName = mid + "_" + UUID.randomUUID().toString().substring(0,4) + "_" + oFileName;

    try {
      // 서버에 파일 구분처리하여 저장하기
      writeFile(fName, sFileName, part);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return sFileName;
  }

  // 지정된 경로에 파일 저장하기
  public void writeFile(MultipartFile fName, String sFileName, String part) throws IOException {
    Path partDir = Paths.get(uploadFolder, part);

    // 폴더 없으면 생성
    if (!Files.exists(partDir)) {
      Files.createDirectories(partDir);
    }

    Path target = partDir.resolve(sFileName);
    try (InputStream in = fName.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  public void fileDelete(String fileName, String part) {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    String realPath = request.getSession().getServletContext().getRealPath("src/main/webapp/upload");
    File file = new File(realPath + fileName);
    if(file.exists()) file.delete();
  }

  // 파일 이름 변경하기(서버 파일시스템에 저장되는 파일명의 중복을 방지하기위함)
  public String saveFileName(String oFileName) {
    Date date = new Date();
    SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
    return sdf.format(date) + "_" + oFileName;
  }

  // 아이디찾기 마스킹
  public static String maskId(String id) {
    if (id == null || id.isEmpty()) return "";

    int length = id.length();
    int visible;

    if (length <= 3) {
      visible = 1;
    } else if (length <= 5) {
      visible = 2;
    } else if (length <= 7) {
      visible = 3;
    } else {
      visible = 4;
    }

    String visiblePart = id.substring(0, visible);
    String maskedPart = "*".repeat(length - visible);

    return visiblePart + maskedPart;
  }
}
