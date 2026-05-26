package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내가 작성한 리뷰 정렬 타입")
public enum MyReviewSortType {
    @Schema(description = "최신순")
    LATEST,

    @Schema(description = "좋아요순")
    LIKE_COUNT
}
