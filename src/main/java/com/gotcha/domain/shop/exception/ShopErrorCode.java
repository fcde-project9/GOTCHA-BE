package com.gotcha.domain.shop.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ShopErrorCode implements ErrorCode {

    SHOP_NOT_FOUND(NOT_FOUND, "S001", "가게를 찾을 수 없습니다"),
    SHOP_ALREADY_EXISTS(CONFLICT, "S002", "이미 등록된 가게입니다"),
    INVALID_SHOP_NAME(BAD_REQUEST, "S003", "가게명은 2-100자여야 합니다"),
    INVALID_COORDINATES(BAD_REQUEST, "S004", "유효하지 않은 좌표입니다"),
    RADIUS_TOO_LARGE(BAD_REQUEST, "S005", "검색 반경은 최대 5000m입니다"),
    KAKAO_API_ERROR(INTERNAL_SERVER_ERROR, "S006", "카카오 API 호출 중 오류가 발생했습니다"),
    ADDRESS_NOT_FOUND(NOT_FOUND, "S007", "해당 좌표의 주소를 찾을 수 없습니다"),
    SHOP_UNAUTHORIZED(FORBIDDEN, "S008", "가게를 수정/삭제할 권한이 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
