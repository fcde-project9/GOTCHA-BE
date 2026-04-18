package com.gotcha.domain.shop.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "가게 검색 결과 (페이지네이션)")
public record ShopSearchResultResponse(
        @Schema(description = "검색 결과 목록")
        List<ShopSearchResponse> content,

        @Schema(description = "전체 결과 수", example = "5")
        long totalCount,

        @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "다음 페이지 존재 여부", example = "false")
        boolean hasNext
) {
    public static ShopSearchResultResponse of(Page<ShopSearchResponse> page) {
        return new ShopSearchResultResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.hasNext()
        );
    }
}
