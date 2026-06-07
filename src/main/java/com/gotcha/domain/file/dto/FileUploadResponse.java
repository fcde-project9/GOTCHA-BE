package com.gotcha.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파일 업로드 응답")
public record FileUploadResponse(

        @Schema(description = "업로드된 파일의 공개 URL", example = "https://d1a2b3c4abcd.cloudfront.net/env/reviews/abc123-def456.webp")
        String fileUrl,

        @Schema(description = "원본 파일명", example = "my-photo.jpg")
        String originalFilename,

        @Schema(description = "파일 크기 (bytes)", example = "102400")
        Long fileSize,

        @Schema(description = "파일 타입", example = "image/webp")
        String contentType,

        @Schema(description = "썸네일 이미지 URL (리스트 뷰용)", nullable = true)
        String thumbnailUrl
) {

    public static FileUploadResponse of(String fileUrl, String originalFilename, Long fileSize, String contentType) {
        return new FileUploadResponse(fileUrl, originalFilename, fileSize, contentType, null);
    }

    public static FileUploadResponse of(String fileUrl, String originalFilename, Long fileSize, String contentType,
                                         String thumbnailUrl) {
        return new FileUploadResponse(fileUrl, originalFilename, fileSize, contentType, thumbnailUrl);
    }
}
