package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 상태 변경 요청")
public record UpdateReportStatusRequest(
    @Schema(description = "변경할 상태 (ACCEPTED, REJECTED)", example = "ACCEPTED")
    @NotNull(message = "상태는 필수입니다")
    ReportStatus status
) {
}
