package com.gotcha.domain.post.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글에 연결된 매장 정보")
public record PostShopInfo(

        @Schema(description = "매장 ID", example = "1")
        Long shopId,

        @Schema(description = "매장 이름", example = "강남 가챠샵")
        String shopName,

        @Schema(description = "매장 주소", example = "서울특별시 강남구 강남대로 364")
        String shopAddress
) {
    public static PostShopInfo from(Shop shop) {
        return new PostShopInfo(shop.getId(), shop.getName(), shop.getAddressName());
    }
}
