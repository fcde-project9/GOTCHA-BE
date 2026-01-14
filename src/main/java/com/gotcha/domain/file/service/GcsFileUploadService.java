package com.gotcha.domain.file.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.exception.FileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Google Cloud Storage 파일 업로드 서비스 (dev, local 환경 전용)
 */
@Slf4j
@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class GcsFileUploadService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/jpg",
            "image/webp",
            "image/heic",  // iOS
            "image/heif"   // iOS
    );
    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "reviews",
            "shops",
            "profiles"
    );

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        validateFolder(folder);

        String filename = generateFilename(file.getOriginalFilename());
        String objectName = folder + "/" + filename;

        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

            log.info("File uploaded successfully to GCS: {}", publicUrl);

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

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String objectName = extractObjectName(fileUrl);
            BlobId blobId = BlobId.of(bucketName, objectName);

            boolean deleted = storage.delete(blobId);

            if (!deleted) {
                log.warn("File not found or already deleted: {}", fileUrl);
            } else {
                log.info("File deleted successfully from GCS: {}", fileUrl);
            }

        } catch (Exception e) {
            log.error("File delete failed: {}", e.getMessage(), e);
            throw FileException.deleteFailed(e.getMessage());
        }
    }

    /**
     * 폴더명 화이트리스트 검증 (Path Traversal 공격 방지)
     */
    private void validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw FileException.uploadFailed("Folder name is required");
        }

        // 경로 조작 문자 차단
        if (folder.contains("..") || folder.contains("/") || folder.contains("\\")) {
            throw FileException.uploadFailed("Invalid folder name: path traversal attempt detected");
        }

        // 화이트리스트 검증
        if (!ALLOWED_FOLDERS.contains(folder.toLowerCase())) {
            throw FileException.uploadFailed("Invalid folder name. Allowed folders: " + ALLOWED_FOLDERS);
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
