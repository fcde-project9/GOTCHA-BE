package com.gotcha.domain.user.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "내가 제보한 가게 응답")
public record MyShopResponse(
    @Schema(description = "가게 ID", example = "1")
    Long id,

    @Schema(description = "가게명", example = "가챠샵 신사점")
    String name,

    @Schema(description = "대표 이미지 URL")
    String mainImageUrl,

    @Schema(description = "주소")
    String addressName,

    @Schema(description = "영업 중 여부", example = "true", nullable = true)
    Boolean isOpen,

    @Schema(description = "제보한 날짜", example = "2025-01-01")
    LocalDate createdAt
) {
    public static MyShopResponse from(Shop shop, Boolean isOpen) {
        return new MyShopResponse(
            shop.getId(),
            shop.getName(),
            shop.getMainImageUrl(),
            shop.getAddressName(),
            isOpen,
            shop.getCreatedAt().toLocalDate()
        );
    }
}
