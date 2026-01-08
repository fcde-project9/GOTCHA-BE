package com.gotcha.domain.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "찜 추가/삭제 응답")
public record FavoriteResponse(
    @Schema(description = "가게 ID", example = "1")
    Long shopId,

    @Schema(description = "찜 상태", example = "true")
    boolean isFavorite
) {
    public static FavoriteResponse of(Long shopId, boolean isFavorite) {
        return new FavoriteResponse(shopId, isFavorite);
    }
}
