package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "가게 응답")
public record ShopResponse(

        @Schema(description = "가게 ID", example = "1")
        Long id,

        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "주소", example = "경기 안성시 죽산면 죽산리 343-1")
        String addressName,

        @Schema(description = "위도", example = "37.5172")
        Double latitude,

        @Schema(description = "경도", example = "127.0473")
        Double longitude,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl,

        @Schema(description = "찾아가는 힌트")
        String locationHint,

        @Schema(description = "시/도", example = "경기")
        String region1DepthName,

        @Schema(description = "시/군/구", example = "안성시")
        String region2DepthName,

        @Schema(description = "읍/면/동 리", example = "죽산면 죽산리")
        String region3DepthName,

        @Schema(description = "본번", example = "343")
        String mainAddressNo,

        @Schema(description = "부번", example = "1")
        String subAddressNo,

        @Schema(description = "생성일시")
        LocalDateTime createdAt
) {

    public static ShopResponse from(Shop shop) {
        return new ShopResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddressName(),
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getMainImageUrl(),
                shop.getLocationHint(),
                shop.getRegion1DepthName(),
                shop.getRegion2DepthName(),
                shop.getRegion3DepthName(),
                shop.getMainAddressNo(),
                shop.getSubAddressNo(),
                shop.getCreatedAt()
        );
    }
}
