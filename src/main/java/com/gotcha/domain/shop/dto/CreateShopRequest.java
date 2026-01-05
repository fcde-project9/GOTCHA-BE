package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "가게 생성 요청")
public record CreateShopRequest(

        @Schema(description = "가게명", example = "가챠샵 신사점")
        @NotBlank(message = "가게명은 필수입니다")
        @Size(min = 2, max = 100, message = "가게명은 2-100자여야 합니다")
        String name,

        @Schema(description = "위도", example = "37.5172")
        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
        Double latitude,

        @Schema(description = "경도", example = "127.0473")
        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
        Double longitude,

        @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg")
        String mainImageUrl,

        @Schema(description = "찾아가는 힌트", example = "신사역 4번 출구에서 도보 3분")
        @Size(max = 500, message = "찾아가는 힌트는 최대 500자입니다")
        String locationHint,

        @Schema(description = "영업시간 (JSON)", example = "{\"mon\":\"10:00-22:00\",\"tue\":\"10:00-22:00\"}")
        String openTime
) {
}
