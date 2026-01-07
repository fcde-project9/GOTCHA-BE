package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "근처 가게 응답")
public record NearbyShopResponse(
        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl
) {
    public static NearbyShopResponse from(Shop shop) {
        return new NearbyShopResponse(
                shop.getName(),
                shop.getMainImageUrl()
        );
    }
}
