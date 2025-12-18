package com.example.springGroupBA.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    // C:/upload/ 값을 가져옴
    @Value("${org.zerock.upload.path}")
    private String uploadRootPath;

    // 파일 저장 (category 파라미터 추가: "member", "board" 등 구분용)
    public String saveFile(MultipartFile file, String memberId, String category) throws IOException {
        if (file == null || file.isEmpty()) return "noimage.jpg";

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) originalFileName = "file";

        // 확장자 분리
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf(".");
        String baseName = originalFileName;
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex);
            baseName = originalFileName.substring(0, dotIndex);
        }

        // 저장할 파일명 (아이디_UUID_파일명.jpg)
        String savedFileName = memberId + "_" + UUID.randomUUID().toString().substring(0, 4) + "_" + baseName + extension;

        // 실제 저장 경로 생성
        Path uploadPath = Paths.get(uploadRootPath, category);

        // 폴더가 없으면 생성 (mkdirs 역할)
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 파일 저장: C:/upload/member/저장된파일명.jpg
        Path targetPath = uploadPath.resolve(savedFileName);
        Files.write(targetPath, file.getBytes());

        return savedFileName;
    }

    // 파일 삭제
    public void deleteFile(String fileName, String category) {
        if (fileName == null || fileName.isEmpty() || "noimage.jpg".equals(fileName)) return;

        // C:/upload/member/파일명
        Path filePath = Paths.get(uploadRootPath, category, fileName);
        File file = filePath.toFile();

        if (file.exists()) {
            file.delete();
        }
    }
}