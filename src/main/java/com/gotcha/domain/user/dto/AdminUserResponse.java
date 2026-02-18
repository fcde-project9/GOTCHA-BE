package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserStatus;
import com.gotcha.domain.user.entity.UserType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "관리자용 사용자 정보 응답")
public record AdminUserResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long id,

    @Schema(description = "닉네임", example = "빨간캡슐#21")
    String nickname,

    @Schema(description = "이메일", example = "user@example.com")
    String email,

    @Schema(description = "프로필 이미지 URL")
    String profileImageUrl,

    @Schema(description = "소셜 로그인 타입", example = "KAKAO")
    SocialType socialType,

    @Schema(description = "사용자 타입", example = "NORMAL")
    UserType userType,

    @Schema(description = "사용자 상태", example = "ACTIVE")
    UserStatus status,

    @Schema(description = "정지 해제 일시 (SUSPENDED 상태일 때만)")
    LocalDateTime suspendedUntil,

    @Schema(description = "마지막 로그인 일시")
    LocalDateTime lastLoginAt,

    @Schema(description = "가입일시")
    LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
            user.getId(),
            user.getNickname(),
            user.getEmail(),
            user.getProfileImageUrl(),
            user.getSocialType(),
            user.getUserType(),
            user.getStatus(),
            user.getSuspendedUntil(),
            user.getLastLoginAt(),
            user.getCreatedAt()
        );
    }
}
