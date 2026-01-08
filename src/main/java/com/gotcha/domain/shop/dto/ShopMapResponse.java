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

        @Schema(description = "운영 시간 (JSON 형식)", example = "{\"AM\":\"10:00\", \"PM\":\"20:00\"}")
        String openTime,

        @Schema(description = "파싱된 운영 시간 (LocalTime)")
        OpenTimeDto parsedOpenTime,

        @Schema(description = "중심 좌표로부터의 거리", example = "50m")
        String distance,

        @Schema(description = "찜 여부 (로그인 사용자만)", example = "false")
        Boolean isFavorite
) {
    /**
     * Shop 엔티티를 ShopMapResponse로 변환
     * @param shop Shop 엔티티
     * @param distance 거리 문자열 (예: "50m", "1.5km")
     * @param parsedOpenTime 파싱된 운영 시간
     * @param isFavorite 찜 여부
     * @return ShopMapResponse
     */
    public static ShopMapResponse of(Shop shop, String distance, OpenTimeDto parsedOpenTime, Boolean isFavorite) {
        return new ShopMapResponse(
                shop.getId(),
                shop.getName(),
                shop.getMainImageUrl(),
                shop.getOpenTime(),
                parsedOpenTime,
                distance,
                isFavorite
        );
    }
}
