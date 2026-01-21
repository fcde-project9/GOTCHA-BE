package com.gotcha.domain.file.service;

import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.exception.FileException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * AWS S3 파일 업로드 서비스 (dev, prod 환경)
 */
@Slf4j
@Service
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class S3FileUploadService implements FileStorageService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
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

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.prefix}")
    private String prefix;

    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        validateFolder(folder);

        String filename = generateFilename(file.getOriginalFilename());
        String key = prefix + folder + "/" + filename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    bucketName, region, key);

            log.info("File uploaded successfully to S3: {}", publicUrl);

            return FileUploadResponse.of(
                    publicUrl,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType()
            );

        } catch (IOException e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            throw FileException.uploadFailed(e.getMessage());
        } catch (S3Exception e) {
            log.error("S3 upload failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw FileException.uploadFailed(e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("Unexpected error during file upload: {}", e.getMessage(), e);
            throw FileException.uploadFailed(e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            String key = extractKey(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            // S3 deleteObject는 멱등성(idempotent) 연산: 객체가 없어도 성공 처리됨
            log.info("File deleted successfully from S3: {}", fileUrl);

        } catch (S3Exception e) {
            log.error("S3 delete failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw FileException.deleteFailed(e.awsErrorDetails().errorMessage());
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
     * S3 URL에서 key 추출
     * (예: https://bucket.s3.region.amazonaws.com/dev/folder/file.jpg -> dev/folder/file.jpg)
     */
    private String extractKey(String fileUrl) {
        String urlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        if (fileUrl.startsWith(urlPrefix)) {
            return fileUrl.substring(urlPrefix.length());
        }
        throw FileException.deleteFailed("Invalid file URL format: " + fileUrl);
    }
}
