package com.gotcha._global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gotcha._global.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorInfo error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, ErrorInfo.of(errorCode));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, ErrorInfo.of(errorCode.getCode(), message));
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, ErrorInfo.of(code, message));
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ErrorInfo {

        private final String code;
        private final String message;

        public static ErrorInfo of(ErrorCode errorCode) {
            return new ErrorInfo(errorCode.getCode(), errorCode.getMessage());
        }

        public static ErrorInfo of(String code, String message) {
            return new ErrorInfo(code, message);
        }
    }
}
