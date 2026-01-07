package com.gotcha.domain.file.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

    FILE_EMPTY(BAD_REQUEST, "FL001", "파일이 비어있습니다"),
    FILE_TOO_LARGE(BAD_REQUEST, "FL002", "파일 크기가 너무 큽니다"),
    UNSUPPORTED_FILE_TYPE(BAD_REQUEST, "FL003", "지원하지 않는 파일 형식입니다"),
    FILE_UPLOAD_FAILED(INTERNAL_SERVER_ERROR, "FL004", "파일 업로드에 실패했습니다"),
    FILE_DELETE_FAILED(INTERNAL_SERVER_ERROR, "FL005", "파일 삭제에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
