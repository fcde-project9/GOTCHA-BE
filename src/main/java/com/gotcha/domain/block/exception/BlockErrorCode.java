package com.gotcha.domain.block.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum BlockErrorCode implements ErrorCode {

    CANNOT_BLOCK_SELF(BAD_REQUEST, "BK001", "본인을 차단할 수 없습니다"),
    ALREADY_BLOCKED(CONFLICT, "BK002", "이미 차단한 사용자입니다"),
    BLOCK_NOT_FOUND(NOT_FOUND, "BK003", "차단 정보를 찾을 수 없습니다"),
    INVALID_BLOCK_TARGET(BAD_REQUEST, "BK004", "차단할 수 없는 사용자입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
