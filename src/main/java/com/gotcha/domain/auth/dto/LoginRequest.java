package com.gotcha.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "액세스 토큰은 필수입니다")
        String accessToken
) {
}
