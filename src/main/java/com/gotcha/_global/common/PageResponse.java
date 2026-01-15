package com.gotcha._global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "페이징 응답")
public record PageResponse<T>(
    @Schema(description = "컨텐츠 목록")
    List<T> content,

    @Schema(description = "전체 개수", example = "100")
    long totalCount,

    @Schema(description = "현재 페이지 번호", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize(),
            page.hasNext()
        );
    }

    public static <T, U> PageResponse<U> from(Page<T> page, List<U> content) {
        return new PageResponse<>(
            content,
            page.getTotalElements(),
            page.getNumber(),
            page.getSize(),
            page.hasNext()
        );
    }
}
