package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "프로필 이미지 변경 요청")
public record UpdateProfileImageRequest(
        @Schema(description = "프로필 이미지 URL (GCS 공개 URL)",
                example = "https://storage.googleapis.com/gotcha-dev-files/profiles/abc-123.webp",
                required = true)
        @NotBlank(message = "프로필 이미지 URL은 필수입니다")
        @Pattern(regexp = "^https://storage\\.googleapis\\.com/.*$",
                 message = "올바른 GCS URL 형식이 아닙니다")
        String profileImageUrl
) {
}
