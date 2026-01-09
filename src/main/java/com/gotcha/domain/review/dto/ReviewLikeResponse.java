package com.gotcha.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "리뷰 좋아요 추가/삭제 응답")
public record ReviewLikeResponse(
    @Schema(description = "리뷰 ID", example = "1")
    Long reviewId,

    @Schema(description = "좋아요 상태", example = "true")
    boolean isLiked
) {
    public static ReviewLikeResponse of(Long reviewId, boolean isLiked) {
        return new ReviewLikeResponse(reviewId, isLiked);
    }
}
