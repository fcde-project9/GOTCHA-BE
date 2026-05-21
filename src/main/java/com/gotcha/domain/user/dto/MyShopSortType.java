package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 제보한 가게 정렬 타입")
public enum MyShopSortType {
    @Schema(description = "최신순")
    LATEST,

    @Schema(description = "좋아요순 (찜한 사용자 수 기준)")
    FAVORITE_COUNT
}
