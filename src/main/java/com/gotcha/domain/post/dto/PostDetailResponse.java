package com.gotcha.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 상세 응답 (댓글/대댓글 포함)")
public record PostDetailResponse(

        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 ID", example = "1")
        Long typeId,

        @Schema(description = "카테고리명", example = "갓챠일상")
        String typeName,

        @Schema(description = "작성자 닉네임", example = "빨간캡슐#21")
        String authorNickname,

        @Schema(description = "본문 내용", example = "오늘 드디어 원하던 캐릭터를 뽑았어요!")
        String content,

        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "연결된 매장 정보 (매장 미지정 시 응답에서 제외)")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PostShopInfo shopInfo,

        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,

        @Schema(description = "좋아요 수", example = "10")
        long likeCount,

        @Schema(description = "좋아요 여부 (비로그인 시 false)", example = "false")
        boolean isLiked,

        @Schema(description = "본인 작성 여부 (비로그인 시 false)", example = "false")
        boolean isOwner,

        @Schema(description = "작성 시간")
        LocalDateTime createdAt,

        @Schema(description = "댓글 목록 (대댓글 포함)")
        List<PostCommentDetailResponse> comments
) {
    public static PostDetailResponse of(
            Post post,
            List<PostImage> images,
            long likeCount,
            boolean isLiked,
            boolean isOwner,
            List<PostCommentDetailResponse> comments
    ) {
        return new PostDetailResponse(
                post.getId(),
                post.getType().getId(),
                post.getType().getTypeName(),
                post.getUser().getNickname(),
                post.getContent(),
                images.stream().map(PostImage::getImageUrl).toList(),
                post.getShop() != null ? PostShopInfo.from(post.getShop()) : null,
                post.isPublic(),
                likeCount,
                isLiked,
                isOwner,
                post.getCreatedAt(),
                comments
        );
    }
}
