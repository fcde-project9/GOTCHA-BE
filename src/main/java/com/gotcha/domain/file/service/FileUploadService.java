package com.gotcha.domain.file.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.exception.FileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
            "image/heic",  // iOS
            "image/heif"   // iOS
    );

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    /**
     * 이미지 파일을 GCS에 업로드
     *
     * @param file   업로드할 파일
     * @param folder 저장할 폴더 (예: "reviews", "shops")
     * @return 업로드된 파일 정보
     */
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);

        String filename = generateFilename(file.getOriginalFilename());
        String objectName = folder + "/" + filename;

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("File uploaded successfully: {}", publicUrl);

            return FileUploadResponse.of(
                    publicUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            throw FileException.uploadFailed(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw FileException.uploadFailed(e.getMessage());
        }
    }

    /**
     * GCS에서 파일 삭제
     *
     * @param fileUrl 삭제할 파일의 공개 URL
     */
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);
            BlobId blobId = BlobId.of(bucketName, objectName);

            boolean deleted = storage.delete(blobId);

            if (!deleted) {
                log.warn("File not found or already deleted: {}", fileUrl);
            } else {
                log.info("File deleted successfully: {}", fileUrl);
            }

        } catch (Exception e) {
            log.error("File delete failed: {}", e.getMessage(), e);
            throw FileException.deleteFailed(e.getMessage());
        }
    }

    /**
     * 파일 유효성 검증 (크기, 타입)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw FileException.empty();
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw FileException.tooLarge(file.getSize(), MAX_FILE_SIZE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw FileException.unsupportedType(contentType);
        }
    }

    /**
     * UUID 기반 고유 파일명 생성
     */
    private String generateFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    /**
     * file URL 에서 GCS 오브젝트명 추출
     * (예: https://storage.googleapis.com/bucket/folder/file.jpg -> folder/file.jpg)
     */
    private String extractObjectName(String fileUrl) {
        String prefix = String.format("https://storage.googleapis.com/%s/", bucketName);
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        throw FileException.deleteFailed("Invalid file URL format: " + fileUrl);
    }
}
