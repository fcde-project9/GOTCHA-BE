package com.gotcha.domain.post.dto;

import com.gotcha.domain.post.entity.PostComment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 댓글 상세 응답 (대댓글 포함)")
public record PostCommentDetailResponse(

        @Schema(description = "댓글 ID", example = "1")
        Long id,

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

        @Schema(description = "좋아요 수", example = "5")
        long likeCount,

        @Schema(description = "좋아요 여부 (비로그인 시 false)", example = "false")
        boolean isLiked,

        @Schema(description = "작성 시간")
        LocalDateTime createdAt,

        @Schema(description = "대댓글 목록 (최상위 댓글에만 존재)")
        List<PostCommentDetailResponse> replies
) {
    public static PostCommentDetailResponse of(
            PostComment comment,
            Long currentUserId,
            long likeCount,
            boolean isLiked,
            List<PostCommentDetailResponse> replies
    ) {
        String nickname = comment.isAnonymous() ? "익명" : comment.getUser().getNickname();
        boolean isOwner = currentUserId != null && comment.getUser().getId().equals(currentUserId);

        return new PostCommentDetailResponse(
                comment.getId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                nickname,
                comment.getContent(),
                comment.isAnonymous(),
                isOwner,
                likeCount,
                isLiked,
                comment.getCreatedAt(),
                replies
        );
    }
}
