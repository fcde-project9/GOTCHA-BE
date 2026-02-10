package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "신고 응답")
public record ReportResponse(
    @Schema(description = "신고 ID", example = "1")
    Long id,

    @Schema(description = "신고 대상 타입", example = "REVIEW")
    ReportTargetType targetType,

    @Schema(description = "신고 대상 ID", example = "1")
    Long targetId,

    @Schema(description = "신고 사유", example = "ABUSE")
    ReportReason reason,

    @Schema(description = "상세 내용")
    String detail,

    @Schema(description = "신고 상태", example = "PENDING")
    ReportStatus status,

    @Schema(description = "생성 시간")
    LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
            report.getId(),
            report.getTargetType(),
            report.getTargetId(),
            report.getReason(),
            report.getDetail(),
            report.getStatus(),
            report.getCreatedAt()
        );
    }
}
