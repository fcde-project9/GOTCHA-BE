package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 정보 응답")
public record UserResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "닉네임", example = "빨간캡슐#21")
        String nickname,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "소셜 로그인 타입", example = "KAKAO")
        SocialType socialType
) {
    public static UserResponse from(User user, String defaultProfileImageUrl) {
        return new UserResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()
                        ? user.getProfileImageUrl()
                        : defaultProfileImageUrl,
                user.getSocialType()
        );
    }
}
