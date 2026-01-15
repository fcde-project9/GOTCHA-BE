package com.gotcha._global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 검증 에러 (VAL_XXX)
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "VAL_001", "필수 필드가 누락되었습니다"),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "VAL_002", "유효하지 않은 형식입니다"),
    INVALID_VALUE(HttpStatus.BAD_REQUEST, "VAL_003", "값이 허용 범위를 벗어났습니다"),

    // 서버 에러 (SERVER_XXX)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_001", "서버 내부 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
