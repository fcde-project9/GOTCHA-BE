package com.gotcha.domain.block.dto;

import com.gotcha.domain.block.entity.UserBlock;
import com.gotcha.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "차단된 사용자 정보")
public record BlockedUserResponse(
    @Schema(description = "사용자 ID", example = "123")
    Long id,

    @Schema(description = "닉네임", example = "빨간캡슐#21")
    String nickname,

    @Schema(description = "프로필 이미지 URL")
    String profileImageUrl,

    @Schema(description = "차단 일시", example = "2025-01-08T12:00:00")
    LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock userBlock) {
        User blocked = userBlock.getBlocked();
        return new BlockedUserResponse(
            blocked.getId(),
            blocked.getNickname(),
            blocked.getProfileImageUrl(),
            userBlock.getCreatedAt()
        );
    }
}
