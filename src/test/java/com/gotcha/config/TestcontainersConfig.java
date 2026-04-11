package com.gotcha.config;

import com.gotcha.domain.file.dto.FileUploadResponse;
import com.gotcha.domain.file.service.FileStorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15-alpine");
    }

    @Bean
    public FileStorageService fileStorageService() {
        return new FileStorageService() {
            @Override
            public FileUploadResponse uploadImage(MultipartFile file, String folder) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                String mockUrl = "https://test-bucket.s3.amazonaws.com/test/" + folder + "/" + fileName;
                return new FileUploadResponse(mockUrl, fileName, file.getSize(), file.getContentType());
            }

            @Override
            public void deleteFile(String fileUrl) {
                // 테스트에서는 실제 삭제 없이 무시
            }
        };
    }
}
