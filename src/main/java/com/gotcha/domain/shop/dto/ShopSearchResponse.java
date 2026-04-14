package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 검색 결과 항목")
public record ShopSearchResponse(
        @Schema(description = "가게 ID", example = "1")
        Long id,

        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "주소", example = "서울시 강남구 신사동 123-45")
        String address,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl,

        @Schema(description = "사용자 위치로부터의 거리(m), 위치 정보 없으면 null", example = "300", nullable = true)
        Integer distance
) {
    public static ShopSearchResponse of(Shop shop, Integer distanceMeters) {
        return new ShopSearchResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddressName(),
                shop.getMainImageUrl(),
                distanceMeters
        );
    }
}
