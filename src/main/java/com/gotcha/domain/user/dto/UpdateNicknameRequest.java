package com.gotcha.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "닉네임 변경 요청")
public record UpdateNicknameRequest(
        @Schema(description = "새 닉네임", example = "파란캡슐#42", required = true)
        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 20, message = "닉네임은 2-20자여야 합니다")
        @Pattern(regexp = "^[가-힣a-zA-Z0-9#]+$", message = "닉네임은 한글, 영문, 숫자, #만 사용할 수 있습니다")
        String nickname
) {
}
