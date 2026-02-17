package com.gotcha.domain.push.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class PushException extends BusinessException {

    private PushException(ErrorCode errorCode) {
        super(errorCode);
    }

    private PushException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static PushException subscriptionNotFound() {
        return new PushException(PushErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    public static PushException subscriptionNotFound(String endpoint) {
        return new PushException(PushErrorCode.SUBSCRIPTION_NOT_FOUND, "endpoint: " + endpoint);
    }

    public static PushException sendFailed() {
        return new PushException(PushErrorCode.PUSH_SEND_FAILED);
    }

    public static PushException sendFailed(String reason) {
        return new PushException(PushErrorCode.PUSH_SEND_FAILED, reason);
    }

    public static PushException vapidKeyNotConfigured() {
        return new PushException(PushErrorCode.VAPID_KEY_NOT_CONFIGURED);
    }
}
