package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 댓글 좋아요 추가/취소 응답")
public record PostCommentLikeResponse(

        @Schema(description = "댓글 ID", example = "1")
        Long commentId,

        @Schema(description = "좋아요 상태", example = "true")
        boolean isLiked
) {
    public static PostCommentLikeResponse of(Long commentId, boolean isLiked) {
        return new PostCommentLikeResponse(commentId, isLiked);
    }
}
