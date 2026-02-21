package com.gotcha.domain.push.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.gotcha._global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PushErrorCode implements ErrorCode {

    SUBSCRIPTION_NOT_FOUND(NOT_FOUND, "P001", "구독 정보를 찾을 수 없습니다"),
    PUSH_SEND_FAILED(INTERNAL_SERVER_ERROR, "P002", "푸시 알림 발송에 실패했습니다"),
    VAPID_KEY_NOT_CONFIGURED(INTERNAL_SERVER_ERROR, "P003", "VAPID 키가 설정되지 않았습니다"),
    DEVICE_TOKEN_NOT_FOUND(NOT_FOUND, "P004", "기기 토큰을 찾을 수 없습니다"),
    APNS_SEND_FAILED(INTERNAL_SERVER_ERROR, "P005", "APNS 알림 발송에 실패했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
