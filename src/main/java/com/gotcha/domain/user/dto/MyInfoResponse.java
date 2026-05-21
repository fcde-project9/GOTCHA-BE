package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 응답")
public record MyInfoResponse(
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

        @Schema(description = "찜한 가게 수", example = "12")
        long favoriteCount,

        @Schema(description = "신고 수", example = "3")
        long reportCount,

        @Schema(description = "리뷰 수", example = "7")
        long reviewCount
) {
    public static MyInfoResponse from(User user, String defaultProfileImageUrl,
            long favoriteCount, long reportCount, long reviewCount) {
        return new MyInfoResponse(
                user.getId(),
                user.getNickname(),
                user.getEmail(),
                user.getProfileImageUrl() != null && !user.getProfileImageUrl().isBlank()
                        ? user.getProfileImageUrl()
                        : defaultProfileImageUrl,
                user.getSocialType(),
                user.getUserType(),
                favoriteCount,
                reportCount,
                reviewCount
        );
    }
}
