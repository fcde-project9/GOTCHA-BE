package com.gotcha.domain.block.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class BlockException extends BusinessException {

    private BlockException(ErrorCode errorCode) {
        super(errorCode);
    }

    private BlockException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static BlockException cannotBlockSelf() {
        return new BlockException(BlockErrorCode.CANNOT_BLOCK_SELF);
    }

    public static BlockException alreadyBlocked() {
        return new BlockException(BlockErrorCode.ALREADY_BLOCKED);
    }

    public static BlockException notFound() {
        return new BlockException(BlockErrorCode.BLOCK_NOT_FOUND);
    }

    public static BlockException notFound(Long blockedUserId) {
        return new BlockException(BlockErrorCode.BLOCK_NOT_FOUND, "blockedUserId: " + blockedUserId);
    }

    public static BlockException invalidBlockTarget() {
        return new BlockException(BlockErrorCode.INVALID_BLOCK_TARGET);
    }

    public static BlockException invalidBlockTarget(Long blockedUserId) {
        return new BlockException(BlockErrorCode.INVALID_BLOCK_TARGET, "blockedUserId: " + blockedUserId);
    }
}
