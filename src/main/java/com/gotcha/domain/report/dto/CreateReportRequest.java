package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "신고 생성 요청")
public record CreateReportRequest(
    @Schema(description = "신고 대상 타입", example = "REVIEW")
    @NotNull(message = "신고 대상 타입은 필수입니다")
    ReportTargetType targetType,

    @Schema(description = "신고 대상 ID", example = "1")
    @NotNull(message = "신고 대상 ID는 필수입니다")
    Long targetId,

    @Schema(description = "신고 사유", example = "ABUSE")
    @NotNull(message = "신고 사유는 필수입니다")
    ReportReason reason,

    @Schema(description = "상세 내용 (기타 사유 선택 시 필수)", example = "부적절한 내용이 포함되어 있습니다")
    String detail
) {
}
