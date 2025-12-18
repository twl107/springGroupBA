package com.example.springGroupBA.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CleanupScheduler {

    @Value("${org.zerock.upload.path}")
    private String uploadPath;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupTempFiles() {
        log.info("============== [전체] 임시 폴더(cktemp) 정리 스케줄러 시작 ==============");

        File dir = new File(uploadPath + "cktemp/");
        if (!dir.exists()) {
            log.info("임시 폴더가 존재하지 않아 스케줄러를 종료합니다.");
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            log.info("정리할 임시 파일이 없습니다.");
            return;
        }

        int deletedCount = 0;
        long retentionPeriod = 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        for (File file : files) {
            if (now - file.lastModified() > retentionPeriod) {
                if (file.delete()) {
                    deletedCount++;
                }
            }
        }
        log.info("============== [전체] 정리 완료 (삭제된 임시파일: {}개) ==============", deletedCount);
    }
}
