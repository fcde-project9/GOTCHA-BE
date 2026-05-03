package com.gotcha.domain.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Schema(description = "게시글 목록 아이템")
public record PostListItemResponse(

        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 ID", example = "1")
        Long typeId,

        @Schema(description = "카테고리명", example = "갓챠일상")
        String typeName,

        @Schema(description = "작성자 ID (차단 등에 사용)", example = "5")
        Long authorId,

        @Schema(description = "작성자 닉네임", example = "빨간캡슐#21")
        String authorNickname,

        @Schema(description = "작성자 프로필 이미지 URL")
        String authorProfileImageUrl,

        @Schema(description = "게시글 내용")
        String content,

        @Schema(description = "이미지 URL 목록 (최대 5개)")
        List<String> imageUrls,

        @Schema(description = "연결된 매장 정보 (매장 미지정 시 응답에서 제외)")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        PostShopInfo shopInfo,

        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,

        @Schema(description = "본인 작성 여부 (비로그인 시 false)", example = "false")
        boolean isOwner,

        @Schema(description = "좋아요 수", example = "12")
        long likeCount,

        @Schema(description = "댓글 수", example = "5")
        long commentCount,

        @Schema(description = "경과 시간 표시 (예: 방금 전, 10분 전, 1시간 전, 1일 전)")
        String timeAgo,

        @Schema(description = "작성 시간")
        LocalDateTime createdAt
) {
    public static PostListItemResponse of(Post post, List<PostImage> images, long likeCount,
                                          long commentCount, Long currentUserId) {
        Long authorId = post.getUser().getId();
        boolean isOwner = currentUserId != null && authorId.equals(currentUserId);
        return new PostListItemResponse(
                post.getId(),
                post.getType().getId(),
                post.getType().getTypeName(),
                authorId,
                post.getUser().getNickname(),
                post.getUser().getProfileImageUrl(),
                post.getContent(),
                images.stream().map(PostImage::getImageUrl).toList(),
                post.getShop() != null ? PostShopInfo.from(post.getShop()) : null,
                post.isPublic(),
                isOwner,
                likeCount,
                commentCount,
                formatTimeAgo(post.getCreatedAt()),
                post.getCreatedAt()
        );
    }

    private static String formatTimeAgo(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(createdAt, now);

        if (minutes < 10) {
            return "방금 전";
        }
        if (minutes < 60) {
            return (minutes / 10 * 10) + "분 전";
        }
        long hours = ChronoUnit.HOURS.between(createdAt, now);
        if (hours < 24) {
            return hours + "시간 전";
        }
        long days = ChronoUnit.DAYS.between(createdAt, now);
        return days + "일 전";
    }
}
