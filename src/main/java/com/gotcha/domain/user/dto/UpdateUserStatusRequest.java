package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "사용자 상태 변경 요청")
public record UpdateUserStatusRequest(
    @Schema(description = "변경할 상태 (ACTIVE, SUSPENDED, BANNED)", example = "SUSPENDED")
    @NotNull(message = "상태는 필수입니다")
    UserStatus status,

    @Schema(description = "정지 기간(시간 단위). SUSPENDED일 때 필수. 허용값: 1, 12, 24, 72, 120, 168, 336, 720",
            example = "24")
    Integer suspensionHours
) {
}
