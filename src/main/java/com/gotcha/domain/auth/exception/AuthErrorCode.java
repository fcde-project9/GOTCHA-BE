package com.gotcha.domain.auth.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "A002", "권한이 없습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다"),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A005", "소셜 로그인에 실패했습니다"),
    UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST, "A006", "지원하지 않는 소셜 로그인입니다"),
    OAUTH_ACCESS_DENIED(HttpStatus.UNAUTHORIZED, "A007", "로그인을 취소했습니다"),
    OAUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A008", "OAuth 토큰이 유효하지 않습니다"),
    OAUTH_INVALID_RESPONSE(HttpStatus.UNAUTHORIZED, "A009", "OAuth 응답을 처리할 수 없습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "A010", "리프레시 토큰을 찾을 수 없습니다"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A011", "리프레시 토큰이 만료되었습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
