package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게시글 댓글/대댓글 작성 요청")
public record CreatePostCommentRequest(

        @Schema(description = "부모 댓글 ID (null이면 댓글, 값이 있으면 대댓글)", example = "null")
        Long parentId,

        @Schema(description = "댓글 내용", example = "좋은 게시글이네요!")
        @NotBlank(message = "댓글 내용은 필수입니다")
        @Size(min = 1, max = 500, message = "댓글은 1-500자여야 합니다")
        String content,

        @Schema(description = "익명 여부", example = "false")
        Boolean isAnonymous
) {
}
