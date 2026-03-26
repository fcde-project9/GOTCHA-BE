package com.gotcha.domain.report.dto;

import com.gotcha.domain.report.entity.ReportReason;
import com.gotcha.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;

/**
 * 신고 사유 목록 응답 DTO
 * - 프론트엔드에서 신고 사유 선택지를 동적으로 구성하기 위해 사용
 */
@Schema(description = "신고 사유 목록 응답")
public record ReportReasonResponse(
        @Schema(description = "신고 대상 유형 코드", example = "SHOP")
        String targetType,

        @Schema(description = "신고 대상 유형 설명", example = "가게")
        String targetTypeDescription,

        @Schema(description = "해당 유형의 신고 사유 목록")
        List<ReasonItem> reasons
) {

    @Schema(description = "신고 사유 항목")
    public record ReasonItem(
            @Schema(description = "신고 사유 코드", example = "SHOP_INAPPROPRIATE")
            String code,

            @Schema(description = "신고 사유 설명 (사용자에게 표시)", example = "부적절한 업체(불법/유해 업소)예요")
            String description
    ) {
        public static ReasonItem from(ReportReason reason) {
            return new ReasonItem(reason.name(), reason.getDescription());
        }
    }

    public static List<ReportReasonResponse> getAllReasons() {
        return Arrays.stream(ReportTargetType.values())
                .map(targetType -> new ReportReasonResponse(
                        targetType.name(),
                        targetType.getDescription(),
                        Arrays.stream(ReportReason.values())
                                .filter(reason -> reason.getTargetType() == targetType)
                                .map(ReasonItem::from)
                                .toList()
                ))
                .toList();
    }
}
