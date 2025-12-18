package com.example.springGroupBA.service.sensor;

import com.example.springGroupBA.dto.sensor.SensorEventDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
@Slf4j
public class ExcelService {

  /**
   * SensorEvent 목록을 Excel(xlsx) 형식으로 출력
   */
  public void writeEventLogExcel(List<SensorEventDto> logs, OutputStream os) throws Exception {

    log.info("📄 Excel Export 시작 - {}건", logs != null ? logs.size() : 0);

    // 워크북 생성
    XSSFWorkbook wb = new XSSFWorkbook();
    XSSFSheet sheet = wb.createSheet("Event Logs");

    int rowIdx = 0;

    // ------------------------------------------------------------
    // 헤더 생성
    // ------------------------------------------------------------
    Row header = sheet.createRow(rowIdx++);
    header.createCell(0).setCellValue("Start Time");
    header.createCell(1).setCellValue("End Time");
    header.createCell(2).setCellValue("Level");
    header.createCell(3).setCellValue("Message");
    header.createCell(4).setCellValue("Raw ID");

    // ------------------------------------------------------------
    // 데이터 row 생성
    // ------------------------------------------------------------
    if (logs != null) {
      for (SensorEventDto log : logs) {
        Row row = sheet.createRow(rowIdx++);

        row.createCell(0).setCellValue(
                log.getStartDatetime() != null ? log.getStartDatetime().toString() : ""
        );
        row.createCell(1).setCellValue(
                log.getEndDatetime() != null ? log.getEndDatetime().toString() : ""
        );
        row.createCell(2).setCellValue(log.getEventLevel());
        row.createCell(3).setCellValue(log.getMessage());
        row.createCell(4).setCellValue(log.getRawId());
      }
    }

    // 컬럼 자동 너비 조정
    for (int i = 0; i < 5; i++) {
      sheet.autoSizeColumn(i);
    }

    // 파일 저장
    wb.write(os);
    wb.close();

    log.info("📄 Excel Export 완료");
  }
}