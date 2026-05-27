package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "프로필 이미지 변경 요청")
public record UpdateProfileImageRequest(
        @Schema(description = "프로필 이미지 URL (클라우드 스토리지 공개 URL)",
                example = "https://gotcha-prod-files.s3.ap-northeast-2.amazonaws.com/prod/profiles/abc-123.webp",
                required = true)
        @NotBlank(message = "프로필 이미지 URL은 필수입니다")
        String profileImageUrl
) {
}
