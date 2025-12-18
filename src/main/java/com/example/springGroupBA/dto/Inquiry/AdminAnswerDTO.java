package com.example.springGroupBA.dto.Inquiry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminAnswerDTO {

    @NotNull(message = "문의 ID는 필수입니다.")
    private Long id;

    @NotBlank(message = "답변 내용응 입력해주세요.")
    private String answerContent;
}
