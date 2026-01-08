package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "근처 가게 목록 응답")
public record NearbyShopsResponse(
        @Schema(description = "근처 가게 개수", example = "3")
        int count,

        @Schema(description = "근처 가게 목록")
        List<NearbyShopResponse> shops
) {
    public static NearbyShopsResponse of(List<NearbyShopResponse> shops) {
        return new NearbyShopsResponse(shops.size(), shops);
    }
}
