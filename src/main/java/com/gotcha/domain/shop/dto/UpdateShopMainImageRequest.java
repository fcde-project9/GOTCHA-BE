package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "가게 대표 이미지 수정 요청")
public record UpdateShopMainImageRequest(

        @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg")
        @NotBlank(message = "이미지 URL은 필수입니다")
        String mainImageUrl
) {
}
