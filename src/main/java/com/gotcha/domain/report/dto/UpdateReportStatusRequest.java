package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 상태 변경 요청")
public record UpdateReportStatusRequest(
    @Schema(description = "변경할 상태 (ACCEPTED, REJECTED)", example = "ACCEPTED")
    @NotNull(message = "상태는 필수입니다")
    ReportStatus status,

    @Schema(description = "제재 상태 (ACCEPTED 시 선택). SUSPENDED 또는 BANNED", example = "SUSPENDED")
    UserStatus userStatus,

    @Schema(description = "정지 기간(시간). userStatus가 SUSPENDED일 때 필수. 허용값: 1, 12, 24, 72, 120, 168, 336, 720",
            example = "24")
    Integer suspensionHours
) {
    public UpdateReportStatusRequest(ReportStatus status) {
        this(status, null, null);
    }
}
