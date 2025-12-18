package com.example.springGroupBA.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReportHistoryDTO {
    private Long id;
    private String reporterMid;
    private String reporterName;
    private String reason;
    private LocalDateTime regDate;
}
