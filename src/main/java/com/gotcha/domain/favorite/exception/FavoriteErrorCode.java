package com.gotcha.domain.favorite.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum FavoriteErrorCode implements ErrorCode {

    ALREADY_FAVORITED(CONFLICT, "F001", "이미 찜한 가게입니다"),
    FAVORITE_NOT_FOUND(NOT_FOUND, "F002", "찜 정보를 찾을 수 없습니다"),
    UNAUTHORIZED_DELETE(FORBIDDEN, "F003", "본인의 찜만 삭제할 수 있습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
