package com.gotcha.domain.review.dto;

import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Schema(description = "리뷰 응답")
public record ReviewResponse(

        @Schema(description = "리뷰 ID", example = "1")
        Long id,

        @Schema(description = "리뷰 내용", example = "원하는 캐릭터 뽑았어요!")
        String content,

        @Schema(description = "이미지 URL 목록", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
        List<String> imageUrls,

        @Schema(description = "작성자 정보")
        AuthorSummary author,

        @Schema(description = "본인 작성 여부", example = "false")
        boolean isOwner,

        @Schema(description = "생성일시", example = "2025-01-01T10:00:00")
        LocalDateTime createdAt
) {

    public static ReviewResponse from(Review review, User author, List<ReviewImage> images, boolean isOwner) {
        return new ReviewResponse(
                review.getId(),
                review.getContent(),
                images.stream()
                        .sorted(Comparator.comparing(ReviewImage::getDisplayOrder))
                        .map(ReviewImage::getImageUrl)
                        .toList(),
                AuthorSummary.from(author),
                isOwner,
                review.getCreatedAt()
        );
    }

    @Schema(description = "작성자 요약 정보")
    public record AuthorSummary(

            @Schema(description = "사용자 ID", example = "1")
            Long id,

            @Schema(description = "닉네임", example = "빨간캡슐#21")
            String nickname,

            @Schema(description = "프로필 이미지 URL")
            String profileImageUrl
    ) {
        public static AuthorSummary from(User user) {
            return new AuthorSummary(
                    user.getId(),
                    user.getNickname(),
                    user.getProfileImageUrl()
            );
        }
    }
}
