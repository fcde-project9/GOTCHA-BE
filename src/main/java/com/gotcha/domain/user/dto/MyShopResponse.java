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

    @Schema(description = "영업 상태", example = "영업 중", allowableValues = {"영업 중", "영업 종료", "휴무", ""})
    String openStatus,

    @Schema(description = "제보한 날짜", example = "2025-01-01")
    LocalDate createdAt
) {
    public static MyShopResponse from(Shop shop, String openStatus) {
        return new MyShopResponse(
            shop.getId(),
            shop.getName(),
            shop.getMainImageUrl(),
            shop.getAddressName(),
            openStatus,
            shop.getCreatedAt().toLocalDate()
        );
    }
}
