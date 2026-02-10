package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.Report;
import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportStatus;
import com.gotcha.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "신고 상세 응답 (관리자용)")
public record ReportDetailResponse(
    @Schema(description = "신고 ID", example = "1")
    Long id,

    @Schema(description = "신고자 ID", example = "1")
    Long reporterId,

    @Schema(description = "신고자 닉네임", example = "빨간캡슐#21")
    String reporterNickname,

    @Schema(description = "신고 대상 타입", example = "REVIEW")
    ReportTargetType targetType,

    @Schema(description = "신고 대상 ID", example = "1")
    Long targetId,

    @Schema(description = "신고 사유", example = "ABUSE")
    ReportReason reason,

    @Schema(description = "신고 사유 설명", example = "욕설/비방")
    String reasonDescription,

    @Schema(description = "상세 내용")
    String detail,

    @Schema(description = "신고 상태", example = "PENDING")
    ReportStatus status,

    @Schema(description = "신고 상태 설명", example = "처리 대기")
    String statusDescription,

    @Schema(description = "생성 시간")
    LocalDateTime createdAt,

    @Schema(description = "수정 시간")
    LocalDateTime updatedAt
) {
    public static ReportDetailResponse from(Report report) {
        return new ReportDetailResponse(
            report.getId(),
            report.getReporter().getId(),
            report.getReporter().getNickname(),
            report.getTargetType(),
            report.getTargetId(),
            report.getReason(),
            report.getReason().getDescription(),
            report.getDetail(),
            report.getStatus(),
            report.getStatus().getDescription(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}
