package com.gotcha.domain.review.dto;

import com.gotcha.domain.review.entity.ReviewImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "리뷰 이미지 목록 응답")
public record ReviewImageListResponse(

        @Schema(description = "총 이미지 개수", example = "25")
        int totalCount,

        @Schema(description = "이미지 URL 목록",
                example = "[\"https://storage.googleapis.com/.../image1.jpg\", \"https://storage.googleapis.com/.../image2.jpg\"]")
        List<String> imageUrls
) {

    public static ReviewImageListResponse from(List<ReviewImage> images) {
        List<String> urls = images.stream()
                .map(ReviewImage::getImageUrl)
                .toList();

        return new ReviewImageListResponse(
                urls.size(),
                urls
        );
    }
}
