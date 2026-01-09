package com.gotcha.domain.user.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class UserException extends BusinessException {

    private UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    private UserException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static UserException duplicateNickname() {
        return new UserException(UserErrorCode.DUPLICATE_NICKNAME);
    }

    public static UserException duplicateNickname(String nickname) {
        return new UserException(UserErrorCode.DUPLICATE_NICKNAME, "nickname: " + nickname);
    }

    public static UserException invalidNicknameFormat() {
        return new UserException(UserErrorCode.INVALID_NICKNAME_FORMAT);
    }

    public static UserException forbiddenNickname() {
        return new UserException(UserErrorCode.FORBIDDEN_NICKNAME);
    }

    public static UserException notFound() {
        return new UserException(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserException notFound(Long userId) {
        return new UserException(UserErrorCode.USER_NOT_FOUND, "ID: " + userId);
    }

    public static UserException alreadyDeleted() {
        return new UserException(UserErrorCode.ALREADY_DELETED);
    }

    public static UserException alreadyDeleted(Long userId) {
        return new UserException(UserErrorCode.ALREADY_DELETED, "ID: " + userId);
    }
}
