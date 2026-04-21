package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 좋아요 추가/취소 응답")
public record PostLikeResponse(

        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "좋아요 상태", example = "true")
        boolean isLiked
) {
    public static PostLikeResponse of(Long postId, boolean isLiked) {
        return new PostLikeResponse(postId, isLiked);
    }
}
