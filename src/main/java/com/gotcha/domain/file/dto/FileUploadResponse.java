package com.gotcha.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 업로드 응답")
public record FileUploadResponse(

        @Schema(description = "업로드된 파일의 공개 URL", example = "https://storage.googleapis.com/gotcha-dev-files/images/uuid.jpg")
        String fileUrl,

        @Schema(description = "원본 파일명", example = "my-photo.jpg")
        String originalFilename,

        @Schema(description = "파일 크기 (bytes)", example = "1024000")
        Long fileSize,

        @Schema(description = "파일 타입", example = "image/jpeg")
        String contentType
) {

    public static FileUploadResponse of(String fileUrl, String originalFilename, Long fileSize, String contentType) {
        return new FileUploadResponse(fileUrl, originalFilename, fileSize, contentType);
    }
}
