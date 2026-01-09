package com.gotcha.domain.review.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class ReviewException extends BusinessException {

    private ReviewException(ErrorCode errorCode) {
        super(errorCode);
    }

    private ReviewException(ErrorCode errorCode, String additionalInfo) {
        super(errorCode, additionalInfo);
    }

    public static ReviewException notFound() {
        return new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND);
    }

    public static ReviewException notFound(Long reviewId) {
        return new ReviewException(ReviewErrorCode.REVIEW_NOT_FOUND, "ID: " + reviewId);
    }

    public static ReviewException unauthorized() {
        return new ReviewException(ReviewErrorCode.UNAUTHORIZED);
    }

    public static ReviewException tooManyImages() {
        return new ReviewException(ReviewErrorCode.TOO_MANY_IMAGES);
    }

    public static ReviewException tooManyImages(int count) {
        return new ReviewException(ReviewErrorCode.TOO_MANY_IMAGES, "제공된 이미지 개수: " + count);
    }

    public static ReviewException alreadyLiked() {
        return new ReviewException(ReviewErrorCode.ALREADY_LIKED);
    }

    public static ReviewException likeNotFound() {
        return new ReviewException(ReviewErrorCode.LIKE_NOT_FOUND);
    }

    public static ReviewException likeNotFound(Long reviewId) {
        return new ReviewException(ReviewErrorCode.LIKE_NOT_FOUND, "reviewId: " + reviewId);
    }
}
