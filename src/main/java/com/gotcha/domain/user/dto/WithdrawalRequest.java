package com.gotcha.domain.user.dto;

import com.gotcha.domain.user.entity.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "회원 탈퇴 요청")
public record WithdrawalRequest(
        @Schema(description = "탈퇴 사유 (복수 선택 가능)", example = "[\"LOW_USAGE\", \"INSUFFICIENT_INFO\"]", required = true)
        @NotEmpty(message = "탈퇴 사유는 최소 1개 이상 선택해야 합니다")
        List<WithdrawalReason> reasons,

        @Schema(description = "기타 사유 상세 (OTHER 선택 시)", example = "직접 입력한 사유")
        @Size(max = 500, message = "상세 사유는 500자 이하여야 합니다")
        String detail
) {
}
