package com.gotcha.domain.review.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements ErrorCode {

    REVIEW_NOT_FOUND(NOT_FOUND, "R001", "리뷰를 찾을 수 없습니다"),
    INVALID_CONTENT_LENGTH(BAD_REQUEST, "R002", "리뷰는 10-1000자여야 합니다"),
    UNAUTHORIZED(FORBIDDEN, "R003", "본인의 리뷰만 수정/삭제할 수 있습니다"),
    TOO_MANY_IMAGES(BAD_REQUEST, "R005", "이미지는 최대 10개까지 첨부 가능합니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
