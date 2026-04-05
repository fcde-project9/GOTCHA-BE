package com.gotcha.domain.post.dto;

import com.gotcha.domain.post.entity.Post;
import com.gotcha.domain.post.entity.PostImage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "게시글 응답")
public record PostResponse(

        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "카테고리 ID", example = "1")
        Long typeId,

        @Schema(description = "카테고리명", example = "갓챠일상")
        String typeName,

        @Schema(description = "작성자 닉네임", example = "빨간캡슐#21")
        String authorNickname,

        @Schema(description = "제목", example = "오늘 갓챠샵 다녀왔어요!")
        String title,

        @Schema(description = "본문 내용", example = "오늘 드디어 원하던 캐릭터를 뽑았어요!")
        String content,

        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,

        @Schema(description = "작성 시간")
        LocalDateTime createdAt
) {
    public static PostResponse from(Post post, List<PostImage> images) {
        return new PostResponse(
                post.getId(),
                post.getType().getId(),
                post.getType().getTypeName(),
                post.getUser().getNickname(),
                post.getTitle(),
                post.getContent(),
                images.stream().map(PostImage::getImageUrl).toList(),
                post.getCreatedAt()
        );
    }
}
