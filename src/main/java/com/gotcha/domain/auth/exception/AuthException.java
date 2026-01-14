package com.gotcha.domain.auth.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class AuthException extends BusinessException {

    private AuthException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static AuthException unauthorized() {
        return new AuthException(AuthErrorCode.UNAUTHORIZED);
    }

    public static AuthException forbidden() {
        return new AuthException(AuthErrorCode.FORBIDDEN);
    }

    public static AuthException tokenExpired() {
        return new AuthException(AuthErrorCode.TOKEN_EXPIRED);
    }

    public static AuthException invalidToken() {
        return new AuthException(AuthErrorCode.INVALID_TOKEN);
    }

    public static AuthException socialLoginFailed() {
        return new AuthException(AuthErrorCode.SOCIAL_LOGIN_FAILED);
    }

    public static AuthException unsupportedSocialType() {
        return new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE);
    }

    public static AuthException refreshTokenNotFound() {
        return new AuthException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    public static AuthException refreshTokenExpired() {
        return new AuthException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    public static AuthException userDeleted() {
        return new AuthException(AuthErrorCode.USER_DELETED);
    }

    public static AuthException invalidAuthCode() {
        return new AuthException(AuthErrorCode.INVALID_AUTH_CODE);
    }
}
