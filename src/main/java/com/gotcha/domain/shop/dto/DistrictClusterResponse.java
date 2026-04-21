package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구별 가게 클러스터 응답")
public record DistrictClusterResponse(

        @Schema(description = "구 이름", example = "강남구")
        String districtName,

        @Schema(description = "가게 수", example = "12")
        Long shopCount,

        @Schema(description = "평균 위도", example = "37.4979")
        Double latitude,

        @Schema(description = "평균 경도", example = "127.0276")
        Double longitude
) {
}
