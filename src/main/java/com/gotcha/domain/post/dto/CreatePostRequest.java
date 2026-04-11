package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "게시글 작성 요청")
public record CreatePostRequest(

        @Schema(description = "카테고리 ID", example = "1")
        @NotNull(message = "카테고리는 필수입니다")
        Long typeId,

        @Schema(description = "제목", example = "오늘 갓챠샵 다녀왔어요!")
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 1, max = 200, message = "제목은 1-200자여야 합니다")
        String title,

        @Schema(description = "본문 내용", example = "오늘 드디어 원하던 캐릭터를 뽑았어요!")
        @NotBlank(message = "본문은 필수입니다")
        @Size(max = 10000, message = "본문은 10000자 이하여야 합니다")
        String content,

        @Schema(description = "이미지 URL 목록 (선택, 최대 5개). null 또는 빈 리스트 = 이미지 없음",
                example = "[\"https://example.com/image1.jpg\"]")
        @Size(max = 5, message = "이미지는 최대 5개까지 첨부 가능합니다")
        List<@NotBlank(message = "이미지 URL은 빈 값일 수 없습니다") String> imageUrls
) {
}
