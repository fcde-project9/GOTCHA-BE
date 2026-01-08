package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지도용 가게 응답")
public record ShopMapResponse(
        @Schema(description = "가게 ID", example = "1")
        Long id,

        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl,

        @Schema(description = "운영 시간 (JSON 형식)", example = "{\"Mon\":\"10:00~22:00\",\"Tue\":null,\"Wed\":\"10:00~22:00\"}")
        String openTime,

        @Schema(description = "영업중 여부 (현재 한국 시간 기준)", example = "true")
        Boolean openStatus,

        @Schema(description = "중심 좌표로부터의 거리", example = "50m")
        String distance,

        @Schema(description = "찜 여부 (로그인 사용자만)", example = "false")
        Boolean isFavorite
) {
    /**
     * Shop 엔티티를 ShopMapResponse로 변환
     * @param shop Shop 엔티티
     * @param distance 거리 문자열 (예: "50m", "1.5km")
     * @param openStatus 영업중 여부
     * @param isFavorite 찜 여부
     * @return ShopMapResponse
     */
    public static ShopMapResponse of(Shop shop, String distance, Boolean openStatus, Boolean isFavorite) {
        return new ShopMapResponse(
                shop.getId(),
                shop.getName(),
                shop.getMainImageUrl(),
                shop.getOpenTime(),
                openStatus,
                distance,
                isFavorite
        );
    }
}
