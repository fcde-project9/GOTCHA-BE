package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "게시글 목록 커서 페이징 응답")
public record PostCursorResponse(

        @Schema(description = "게시글 목록")
        List<PostListItemResponse> content,

        @Schema(description = "다음 페이지 커서 (마지막 게시글 ID, 다음 데이터 없으면 null)", example = "42")
        Long nextCursor,

        @Schema(description = "다음 데이터 존재 여부", example = "true")
        boolean hasNext
) {
    public static PostCursorResponse of(List<PostListItemResponse> content, boolean hasNext) {
        Long nextCursor = (hasNext && !content.isEmpty())
                ? content.get(content.size() - 1).id()
                : null;
        return new PostCursorResponse(content, nextCursor, hasNext);
    }
}
