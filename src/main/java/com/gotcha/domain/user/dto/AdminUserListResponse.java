package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

@Schema(description = "관리자용 사용자 목록 응답")
public record AdminUserListResponse(
    @Schema(description = "사용자 목록")
    List<AdminUserResponse> users,

    @Schema(description = "현재 페이지 (0부터 시작)", example = "0")
    int page,

    @Schema(description = "페이지 크기", example = "20")
    int size,

    @Schema(description = "총 항목 수", example = "100")
    long totalElements,

    @Schema(description = "총 페이지 수", example = "5")
    int totalPages,

    @Schema(description = "마지막 페이지 여부", example = "false")
    boolean last
) {
    public static AdminUserListResponse from(Page<AdminUserResponse> userPage) {
        return new AdminUserListResponse(
            userPage.getContent(),
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            userPage.isLast()
        );
    }
}
