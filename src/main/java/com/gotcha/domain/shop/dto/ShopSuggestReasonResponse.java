package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.SuggestionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.List;

/**
 * 가게 정보 수정 제안 사유 목록 응답 DTO
 * - 프론트엔드에서 제안 사유 선택지를 동적으로 구성하기 위해 사용
 */
@Schema(description = "가게 정보 수정 제안 사유 항목")
public record ShopSuggestReasonResponse(
        @Schema(description = "제안 사유 코드", example = "WRONG_ADDRESS")
        String code,

        @Schema(description = "제안 사유 설명 (사용자에게 표시)", example = "주소 정보가 잘못됐어요")
        String description
) {

    public static ShopSuggestReasonResponse from(SuggestionReason reason) {
        return new ShopSuggestReasonResponse(reason.name(), reason.getDescription());
    }

    public static List<ShopSuggestReasonResponse> getAllReasons() {
        return Arrays.stream(SuggestionReason.values())
                .map(ShopSuggestReasonResponse::from)
                .toList();
    }
}
