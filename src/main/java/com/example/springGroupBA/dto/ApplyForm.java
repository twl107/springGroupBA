package com.example.springGroupBA.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ApplyForm {
  private String name;
  private String email;
  private String phone;
  private String intro;
  private MultipartFile resume;
  private String position;
}

