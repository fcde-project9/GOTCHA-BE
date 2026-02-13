package com.gotcha.domain.block.dto;

import com.gotcha.domain.block.entity.UserBlock;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 차단 응답")
public record BlockResponse(
    @Schema(description = "차단된 사용자 ID", example = "123")
    Long blockedUserId,

    @Schema(description = "차단 일시", example = "2025-01-08T12:00:00")
    LocalDateTime blockedAt
) {
    public static BlockResponse from(UserBlock userBlock) {
        return new BlockResponse(
            userBlock.getBlocked().getId(),
            userBlock.getCreatedAt()
        );
    }
}
