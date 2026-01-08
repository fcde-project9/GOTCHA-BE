package com.gotcha.domain.favorite.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class FavoriteException extends BusinessException {

    private FavoriteException(ErrorCode errorCode) {
        super(errorCode);
    }

    private FavoriteException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static FavoriteException alreadyFavorited() {
        return new FavoriteException(FavoriteErrorCode.ALREADY_FAVORITED);
    }

    public static FavoriteException notFound() {
        return new FavoriteException(FavoriteErrorCode.FAVORITE_NOT_FOUND);
    }

    public static FavoriteException notFound(Long shopId) {
        return new FavoriteException(FavoriteErrorCode.FAVORITE_NOT_FOUND, "shopId: " + shopId);
    }

    public static FavoriteException unauthorizedDelete() {
        return new FavoriteException(FavoriteErrorCode.UNAUTHORIZED_DELETE);
    }
}
