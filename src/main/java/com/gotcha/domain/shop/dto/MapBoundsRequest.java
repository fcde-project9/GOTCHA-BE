package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Schema(description = "지도 영역 경계 요청")
public record MapBoundsRequest(
        @Schema(description = "북동쪽 위도", example = "37.5200", required = true)
        @NotNull(message = "북동쪽 위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double northEastLat,

        @Schema(description = "북동쪽 경도", example = "127.0500", required = true)
        @NotNull(message = "북동쪽 경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double northEastLng,

        @Schema(description = "남서쪽 위도", example = "37.5100", required = true)
        @NotNull(message = "남서쪽 위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double southWestLat,

        @Schema(description = "남서쪽 경도", example = "127.0400", required = true)
        @NotNull(message = "남서쪽 경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double southWestLng,

        @Schema(description = "사용자 현재 위치 위도 (거리 계산 기준)", example = "37.5150")
        @NotNull(message = "사용자 위치 위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double latitude,

        @Schema(description = "사용자 현재 위치 경도 (거리 계산 기준)", example = "127.0450")
        @NotNull(message = "사용자 위치 경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double longitude
) {
}
