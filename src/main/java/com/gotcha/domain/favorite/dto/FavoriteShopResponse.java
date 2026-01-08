package com.gotcha.domain.favorite.dto;

import com.gotcha.domain.favorite.entity.Favorite;
import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "찜한 가게 응답")
public record FavoriteShopResponse(
    @Schema(description = "가게 ID", example = "1")
    Long id,

    @Schema(description = "가게명", example = "가챠샵 신사점")
    String name,

    @Schema(description = "주소", example = "서울시 강남구 신사동 123-45")
    String address,

    @Schema(description = "대표 이미지 URL")
    String mainImageUrl,

    @Schema(description = "거리(미터)", example = "300")
    Integer distance,

    @Schema(description = "영업 중 여부", example = "true")
    Boolean isOpen,

    @Schema(description = "찜한 일시")
    LocalDateTime favoritedAt
) {
    public static FavoriteShopResponse from(Favorite favorite, Integer distance, Boolean isOpen) {
        Shop shop = favorite.getShop();
        return new FavoriteShopResponse(
            shop.getId(),
            shop.getName(),
            shop.getAddressName(),
            shop.getMainImageUrl(),
            distance,
            isOpen,
            favorite.getCreatedAt()
        );
    }
}
