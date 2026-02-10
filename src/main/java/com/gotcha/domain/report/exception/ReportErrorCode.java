package com.gotcha.domain.report.exception;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements ErrorCode {

    REPORT_NOT_FOUND(NOT_FOUND, "RP001", "신고를 찾을 수 없습니다"),
    ALREADY_REPORTED(CONFLICT, "RP002", "이미 신고한 대상입니다"),
    TARGET_NOT_FOUND(NOT_FOUND, "RP003", "신고 대상을 찾을 수 없습니다"),
    CANNOT_REPORT_SELF(BAD_REQUEST, "RP004", "본인을 신고할 수 없습니다"),
    DETAIL_REQUIRED_FOR_OTHER(BAD_REQUEST, "RP005", "기타 사유 선택 시 상세 내용을 입력해주세요"),
    UNAUTHORIZED_CANCEL(FORBIDDEN, "RP006", "본인의 신고만 취소할 수 있습니다"),
    ALREADY_PROCESSED(BAD_REQUEST, "RP007", "이미 처리된 신고는 취소할 수 없습니다"),
    ADMIN_ONLY(FORBIDDEN, "RP008", "관리자만 접근할 수 있습니다"),
    INVALID_REASON_FOR_TARGET(BAD_REQUEST, "RP009", "해당 신고 대상에 사용할 수 없는 사유입니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
