package com.gotcha.domain.post.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements ErrorCode {

    POST_NOT_FOUND(NOT_FOUND, "PT001", "게시글을 찾을 수 없습니다"),
    POST_TYPE_NOT_FOUND(NOT_FOUND, "PT002", "카테고리를 찾을 수 없습니다"),
    TOO_MANY_IMAGES(BAD_REQUEST, "PT003", "이미지는 최대 5개까지 첨부 가능합니다"),
    UNAUTHORIZED(FORBIDDEN, "PT004", "본인의 게시글만 수정/삭제할 수 있습니다"),
    ALREADY_LIKED(CONFLICT, "PT005", "이미 좋아요한 게시글입니다"),
    LIKE_NOT_FOUND(NOT_FOUND, "PT006", "좋아요를 찾을 수 없습니다"),
    COMMENT_NOT_FOUND(NOT_FOUND, "PT007", "댓글을 찾을 수 없습니다"),
    COMMENT_UNAUTHORIZED(FORBIDDEN, "PT008", "본인의 댓글만 수정/삭제할 수 있습니다"),
    REPLY_DEPTH_EXCEEDED(BAD_REQUEST, "PT009", "대댓글에는 댓글을 달 수 없습니다"),
    COMMENT_ALREADY_LIKED(CONFLICT, "PT010", "이미 좋아요한 댓글입니다"),
    COMMENT_LIKE_NOT_FOUND(NOT_FOUND, "PT011", "댓글 좋아요를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
