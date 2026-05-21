package com.gotcha.domain.post.dto;

import com.gotcha.domain.post.entity.PostComment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "게시글 댓글 응답")
public record PostCommentResponse(

        @Schema(description = "댓글 ID", example = "1")
        Long id,

        @Schema(description = "게시글 ID", example = "1")
        Long postId,

        @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "null")
        Long parentId,

        @Schema(description = "작성자 닉네임 (익명이면 '익명')", example = "빨간캡슐#21")
        String authorNickname,

        @Schema(description = "댓글 내용", example = "좋은 게시글이네요!")
        String content,

        @Schema(description = "익명 여부", example = "false")
        boolean isAnonymous,

        @Schema(description = "본인 작성 여부", example = "true")
        boolean isOwner,

        @Schema(description = "작성 시간")
        LocalDateTime createdAt
) {
    public static PostCommentResponse from(PostComment comment, Long currentUserId) {
        String nickname = comment.isAnonymous() ? "익명" : comment.getUser().getNickname();
        boolean isOwner = currentUserId != null && comment.getUser().getId().equals(currentUserId);

        return new PostCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                nickname,
                comment.getContent(),
                comment.isAnonymous(),
                isOwner,
                comment.getCreatedAt()
        );
    }
}
