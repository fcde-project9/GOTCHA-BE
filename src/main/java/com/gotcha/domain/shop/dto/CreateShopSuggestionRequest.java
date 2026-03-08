package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.SuggestionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "가게 정보 수정 제안 요청")
public record CreateShopSuggestionRequest(
    @Schema(description = "제안 사유 목록 (복수 선택 가능)", example = "[\"WRONG_ADDRESS\", \"WRONG_PHOTO\"]")
    @NotEmpty(message = "제안 사유를 1개 이상 선택해주세요")
    List<SuggestionReason> reasons
) {
}
