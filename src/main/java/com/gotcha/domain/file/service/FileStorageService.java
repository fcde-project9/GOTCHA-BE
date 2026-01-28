package com.gotcha.domain.file.service;

import com.gotcha.domain.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 스토리지 서비스 인터페이스
 * File Storage Service : (local/dev/prod: AWS S3)
 */
public interface FileStorageService {

    /**
     * 이미지 파일을 스토리지에 업로드
     *
     * @param file   업로드할 파일
     * @param folder 저장할 폴더 (예: "reviews", "shops", "profiles")
     * @return 업로드된 파일 정보
     */
    FileUploadResponse uploadImage(MultipartFile file, String folder);

    /**
     * 스토리지에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 공개 URL
     */
    void deleteFile(String fileUrl);
}
