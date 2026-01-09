package com.gotcha.domain.user.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    DUPLICATE_NICKNAME(CONFLICT, "U001", "이미 사용 중인 닉네임입니다"),
    INVALID_NICKNAME_FORMAT(BAD_REQUEST, "U002", "닉네임 형식이 올바르지 않습니다"),
    FORBIDDEN_NICKNAME(BAD_REQUEST, "U003", "사용할 수 없는 닉네임입니다"),
    USER_NOT_FOUND(NOT_FOUND, "U004", "사용자를 찾을 수 없습니다"),
    ALREADY_DELETED(BAD_REQUEST, "U005", "이미 탈퇴한 사용자입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
