package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가게 상세 조회 응답")
public record ShopDetailResponse(
        @Schema(description = "가게 ID", example = "1")
        Long id,

        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "주소", example = "서울시 강남구 신사동 123-45")
        String addressName,

        @Schema(description = "운영 시간 (JSON 형식)", example = "{\"Mon\":\"10:00~22:00\",\"Tue\":null,\"Wed\":\"10:00~22:00\"}")
        String openTime,

        @Schema(description = "위도", example = "37.517305")
        Double latitude,

        @Schema(description = "경도", example = "127.022775")
        Double longitude,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl,

        @Schema(description = "찜 여부 (로그인 사용자만)", example = "false")
        Boolean isFavorite
) {
    /**
     * Shop 엔티티를 ShopDetailResponse로 변환
     * @param shop Shop 엔티티
     * @param isFavorite 찜 여부
     * @return ShopDetailResponse
     */
    public static ShopDetailResponse of(Shop shop, Boolean isFavorite) {
        return new ShopDetailResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddressName(),
                shop.getOpenTime(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getMainImageUrl(),
                isFavorite
        );
    }
}
