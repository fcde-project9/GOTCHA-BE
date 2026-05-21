package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "게시글 수정 요청")
public record UpdatePostRequest(

        @Schema(description = "카테고리 ID", example = "1")
        @NotNull(message = "카테고리는 필수입니다")
        Long typeId,

        @Schema(description = "연결할 매장 ID (선택, null 보내면 매장 연결 해제)", example = "1")
        Long shopId,

        @Schema(description = "본문 내용", example = "수정된 본문 내용입니다")
        @NotBlank(message = "본문은 필수입니다")
        @Size(max = 10000, message = "본문은 10000자 이하여야 합니다")
        String content,

        @Schema(description = "이미지 URL 목록 (최대 5개). null 또는 빈 리스트 = 이미지 없음. 기존 이미지는 모두 대체됨",
                example = "[\"https://example.com/image1.jpg\"]")
        @Size(max = 5, message = "이미지는 최대 5개까지 첨부 가능합니다")
        List<@NotBlank(message = "이미지 URL은 빈 값일 수 없습니다") String> imageUrls
) {
}
