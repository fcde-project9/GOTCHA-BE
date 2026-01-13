package com.gotcha.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * OAuth 임시 코드를 토큰으로 교환하기 위한 요청 DTO
 */
public record TokenExchangeRequest(
        @NotBlank(message = "code는 필수입니다")
        String code
) {
}
