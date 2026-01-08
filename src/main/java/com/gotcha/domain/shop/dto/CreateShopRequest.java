package com.gotcha.domain.shop.dto;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "가게 생성 요청", example = """
        {
          "name": "가챠샵 신사점",
          "mainImageUrl": "https://example.com/image.jpg",
          "locationHint": "신사역 4번 출구에서 도보 3분",
          "openTime": {
            "Mon": "10:00~22:00",
            "Tue": null,
            "Wed": "10:00~22:00",
            "Thu": "10:00~22:00",
            "Fri": "10:00~22:30",
            "Sat": "10:00~22:30",
            "Sun": "10:00~22:30"
          }
        }
        """)
public record CreateShopRequest(

        @Schema(description = "가게명", example = "가챠샵 신사점")
        @NotBlank(message = "가게명은 필수입니다")
        @Size(min = 2, max = 100, message = "가게명은 2-100자여야 합니다")
        String name,

        @Schema(description = "대표 이미지 URL", example = "https://example.com/image.jpg")
        String mainImageUrl,

        @Schema(description = "찾아가는 힌트", example = "신사역 4번 출구에서 도보 3분")
        @Size(max = 500, message = "찾아가는 힌트는 최대 500자입니다")
        String locationHint,

        @Schema(
                description = "운영 시간 (요일별 영업시간, 휴무일은 null)",
                example = "{\"Mon\":\"10:00~22:00\",\"Tue\":null,\"Wed\":\"10:00~22:00\",\"Thu\":\"10:00~22:00\",\"Fri\":\"10:00~22:30\",\"Sat\":\"10:00~22:30\",\"Sun\":\"10:00~22:30\"}"
        )
        Map<String, String> openTime
) {
}
