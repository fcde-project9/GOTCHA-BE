package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;

@Schema(description = "운영 시간")
public record OpenTimeDto(
        @Schema(description = "오픈 시간", example = "10:00:00")
        LocalTime openAm,

        @Schema(description = "마감 시간", example = "20:00:00")
        LocalTime closePm
) {
}
