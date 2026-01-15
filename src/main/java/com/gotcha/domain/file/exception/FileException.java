package com.gotcha.domain.file.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class FileException extends BusinessException {

    private FileException(ErrorCode errorCode) {
        super(errorCode);
    }

    private FileException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static FileException empty() {
        return new FileException(FileErrorCode.FILE_EMPTY);
    }

    public static FileException tooLarge() {
        return new FileException(FileErrorCode.FILE_TOO_LARGE);
    }

    public static FileException tooLarge(long size, long maxSize) {
        return new FileException(FileErrorCode.FILE_TOO_LARGE,
                "size: " + size + "bytes, max: " + maxSize + "bytes");
    }

    public static FileException unsupportedType() {
        return new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    public static FileException unsupportedType(String contentType) {
        return new FileException(FileErrorCode.UNSUPPORTED_FILE_TYPE,
                "contentType: " + contentType);
    }

    public static FileException uploadFailed() {
        return new FileException(FileErrorCode.FILE_UPLOAD_FAILED);
    }

    public static FileException uploadFailed(String message) {
        return new FileException(FileErrorCode.FILE_UPLOAD_FAILED, message);
    }

    public static FileException deleteFailed() {
        return new FileException(FileErrorCode.FILE_DELETE_FAILED);
    }

    public static FileException deleteFailed(String message) {
        return new FileException(FileErrorCode.FILE_DELETE_FAILED, message);
    }
}
