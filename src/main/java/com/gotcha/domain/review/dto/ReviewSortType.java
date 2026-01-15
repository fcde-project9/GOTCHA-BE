package com.gotcha.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리뷰 정렬 타입")
public enum ReviewSortType {
    @Schema(description = "최신순")
    LATEST,

    @Schema(description = "좋아요순")
    LIKE_COUNT
}
