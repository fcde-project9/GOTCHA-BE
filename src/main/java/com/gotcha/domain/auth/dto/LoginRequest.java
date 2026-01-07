package com.gotcha.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "accessToken은 필수입니다")
        String accessToken
) {
}
