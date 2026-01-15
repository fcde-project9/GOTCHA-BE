package com.gotcha.domain.auth.dto;

/**
 * OAuth 임시 코드 교환 응답 DTO
 */
public record TokenExchangeResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {
    public static TokenExchangeResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return new TokenExchangeResponse(accessToken, refreshToken, isNewUser);
    }
}
