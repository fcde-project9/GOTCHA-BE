package com.gotcha.domain.user.dto;

import com.gotcha.domain.review.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내가 작성한 리뷰 응답")
public record MyReviewResponse(
    @Schema(description = "리뷰 ID", example = "1")
    Long id,

    @Schema(description = "가게 ID", example = "5")
    Long shopId,

    @Schema(description = "가게명", example = "가챠샵 신사점")
    String shopName,

    @Schema(description = "리뷰 내용")
    String content,

    @Schema(description = "리뷰 이미지 URL 목록")
    List<String> imageUrls,

    @Schema(description = "좋아요 수", example = "12")
    Long likeCount,

    @Schema(description = "내가 좋아요 눌렀는지 여부", example = "false")
    boolean isLiked,

    @Schema(description = "작성 일시", example = "2025-01-01T10:30:00")
    LocalDateTime createdAt
) {
    public static MyReviewResponse from(
            Review review,
            List<String> imageUrls,
            Long likeCount,
            boolean isLiked
    ) {
        return new MyReviewResponse(
            review.getId(),
            review.getShop().getId(),
            review.getShop().getName(),
            review.getContent(),
            imageUrls,
            likeCount,
            isLiked,
            review.getCreatedAt()
        );
    }
}
