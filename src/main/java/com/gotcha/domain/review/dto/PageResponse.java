package com.gotcha.domain.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "페이징 응답")
public record PageResponse<T>(

        @Schema(description = "데이터 목록")
        List<T> content,

        @Schema(description = "전체 데이터 개수", example = "100")
        long totalCount,

        @Schema(description = "페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return new PageResponse<>(
                content,
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}
