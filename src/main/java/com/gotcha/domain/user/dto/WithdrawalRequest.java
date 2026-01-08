package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 탈퇴 요청")
public record WithdrawalRequest(
        @Schema(description = "탈퇴 사유", example = "LOW_USAGE", required = true)
        @NotNull(message = "탈퇴 사유는 필수입니다")
        WithdrawalReason reason,

        @Schema(description = "상세 사유 (기타 선택 시)", example = "더 이상 사용하지 않아요")
        @Size(max = 500, message = "상세 사유는 500자 이하여야 합니다")
        String detail
) {
}
