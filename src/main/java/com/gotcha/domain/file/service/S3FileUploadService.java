package com.gotcha.domain.file.service;

import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.exception.FileException;
import com.gotcha.domain.file.service.ImageProcessingService.ProcessedImageResult;
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

@Slf4j
@Service
@Profile({"local", "dev", "prod"})
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
            "profiles",
            "posts"
    );

    private final S3Client s3Client;
    private final ImageProcessingService imageProcessingService;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.prefix}")
    private String prefix;

    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        validateFile(file);
        validateFolder(folder);

        String uuid = UUID.randomUUID().toString();
        String normalizedPrefix = (prefix == null || prefix.isBlank())
                ? ""
                : (prefix.endsWith("/") ? prefix : prefix + "/");

        try {
            byte[] originalBytes = file.getBytes();
            ProcessedImageResult processed = imageProcessingService.process(originalBytes, file.getContentType());

            byte[] mainBytes;
            String contentType;
            String extension;
            byte[] thumbBytes = null;

            if (processed != null) {
                mainBytes = processed.mainImageBytes();
                contentType = processed.contentType();
                extension = processed.extension();
                thumbBytes = processed.thumbnailBytes();
            } else {
                mainBytes = originalBytes;
                contentType = file.getContentType();
                extension = extractExtension(file.getOriginalFilename());
            }

            String mainKey = normalizedPrefix + folder + "/" + uuid + extension;
            uploadToS3(mainKey, mainBytes, contentType);
            String mainUrl = buildPublicUrl(mainKey);

            log.info("File uploaded to S3. URL: {}, Key: {}", mainUrl, mainKey);

            String thumbnailUrl = null;
            if (thumbBytes != null) {
                String thumbKey = normalizedPrefix + folder + "/" + uuid + "_thumb" + extension;
                uploadToS3(thumbKey, thumbBytes, contentType);
                thumbnailUrl = buildPublicUrl(thumbKey);
                log.info("Thumbnail uploaded to S3. URL: {}", thumbnailUrl);
            }

            return FileUploadResponse.of(
                    mainUrl,
                    file.getOriginalFilename(),
                    (long) mainBytes.length,
                    contentType,
                    thumbnailUrl
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
            log.info("Attempting to delete file from S3. URL: {}", fileUrl);

            String key = extractKey(fileUrl);
            log.info("Extracted key for deletion: {}", key);

            deleteFromS3(key);
            log.info("File deleted from S3. Bucket: {}, Key: {}", bucketName, key);

            try {
                String thumbKey = deriveThumbKey(key);
                if (thumbKey != null) {
                    deleteFromS3(thumbKey);
                    log.debug("Thumbnail deleted from S3. Key: {}", thumbKey);
                }
            } catch (Exception e) {
                log.warn("Thumbnail deletion failed (non-critical). URL: {}", fileUrl);
            }

        } catch (S3Exception e) {
            log.error("S3 delete failed for URL: {}. Error: {}", fileUrl, e.awsErrorDetails().errorMessage(), e);
            throw FileException.deleteFailed(e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("File delete failed for URL: {}. Error: {}", fileUrl, e.getMessage(), e);
            throw FileException.deleteFailed(e.getMessage());
        }
    }

    private void uploadToS3(String key, byte[] bytes, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(bytes));
    }

    private void deleteFromS3(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    private String buildPublicUrl(String key) {
        return (cloudfrontDomain != null && !cloudfrontDomain.isBlank())
                ? String.format("https://%s/%s", cloudfrontDomain, key)
                : String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
    }

    private String deriveThumbKey(String key) {
        int dotIndex = key.lastIndexOf('.');
        if (dotIndex > 0) {
            return key.substring(0, dotIndex) + "_thumb" + key.substring(dotIndex);
        }
        return null;
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return "";
    }

    private void validateFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw FileException.uploadFailed("Folder name is required");
        }

        if (folder.contains("..") || folder.contains("/") || folder.contains("\\")) {
            throw FileException.uploadFailed("Invalid folder name: path traversal attempt detected");
        }

        if (!ALLOWED_FOLDERS.contains(folder.toLowerCase())) {
            throw FileException.uploadFailed("Invalid folder name. Allowed folders: " + ALLOWED_FOLDERS);
        }
    }

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

    private String extractKey(String fileUrl) {
        if (cloudfrontDomain != null && !cloudfrontDomain.isBlank()) {
            String cfPrefix = String.format("https://%s/", cloudfrontDomain);
            if (fileUrl.startsWith(cfPrefix)) {
                String extractedKey = fileUrl.substring(cfPrefix.length());
                log.debug("Extracted key from CloudFront URL: {}", extractedKey);
                return extractedKey;
            }
        }

        String s3Prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, region);
        log.debug("Extracting key from URL. Expected prefix: {}, Actual URL: {}", s3Prefix, fileUrl);

        if (fileUrl.startsWith(s3Prefix)) {
            String extractedKey = fileUrl.substring(s3Prefix.length());
            log.debug("Extracted key from S3 URL: {}", extractedKey);
            return extractedKey;
        }

        log.error("URL format mismatch! Actual URL: {}", fileUrl);
        throw FileException.deleteFailed("Invalid file URL format. Actual: " + fileUrl);
    }
}
