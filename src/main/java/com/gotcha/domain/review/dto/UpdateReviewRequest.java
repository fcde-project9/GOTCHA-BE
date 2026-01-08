package com.gotcha.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "리뷰 수정 요청")
public record UpdateReviewRequest(

        @Schema(description = "리뷰 내용", example = "수정된 리뷰 내용입니다!")
        @NotBlank(message = "리뷰 내용은 필수입니다")
        @Size(min = 10, max = 1000, message = "리뷰는 10-1000자여야 합니다")
        String content,

        @Schema(description = "이미지 URL 목록 (선택, 최대 10개). null 또는 빈 리스트 = 모든 이미지 삭제", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]")
        @Size(max = 10, message = "이미지는 최대 10개까지 첨부 가능합니다")
        List<@NotBlank(message = "이미지 URL은 빈 값일 수 없습니다") String> imageUrls
) {
}
