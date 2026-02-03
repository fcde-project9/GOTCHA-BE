package com.gotcha.domain.shop.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class ShopException extends BusinessException {

    private ShopException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ShopException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static ShopException notFound() {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND);
    }

    public static ShopException notFound(Long shopId) {
        return new ShopException(ShopErrorCode.SHOP_NOT_FOUND, "ID: " + shopId);
    }

    public static ShopException alreadyExists() {
        return new ShopException(ShopErrorCode.SHOP_ALREADY_EXISTS);
    }

    public static ShopException invalidName() {
        return new ShopException(ShopErrorCode.INVALID_SHOP_NAME);
    }

    public static ShopException invalidCoordinates() {
        return new ShopException(ShopErrorCode.INVALID_COORDINATES);
    }

    public static ShopException radiusTooLarge() {
        return new ShopException(ShopErrorCode.RADIUS_TOO_LARGE);
    }

    public static ShopException kakaoApiError() {
        return new ShopException(ShopErrorCode.KAKAO_API_ERROR);
    }

    public static ShopException kakaoApiError(String message) {
        return new ShopException(ShopErrorCode.KAKAO_API_ERROR, message);
    }

    public static ShopException addressNotFound() {
        return new ShopException(ShopErrorCode.ADDRESS_NOT_FOUND);
    }

    public static ShopException addressNotFound(Double latitude, Double longitude) {
        return new ShopException(ShopErrorCode.ADDRESS_NOT_FOUND,
                "lat: " + latitude + ", lng: " + longitude);
    }

    public static ShopException unauthorized() {
        return new ShopException(ShopErrorCode.SHOP_UNAUTHORIZED);
    }
}
