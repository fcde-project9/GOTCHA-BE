package com.gotcha.domain.post.exception;

import com.gotcha._global.exception.BusinessException;
import com.gotcha._global.exception.ErrorCode;

public class PostException extends BusinessException {

    private PostException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static PostException notFound() {
        return new PostException(PostErrorCode.POST_NOT_FOUND);
    }

    public static PostException typeNotFound() {
        return new PostException(PostErrorCode.POST_TYPE_NOT_FOUND);
    }

    public static PostException tooManyImages() {
        return new PostException(PostErrorCode.TOO_MANY_IMAGES);
    }

    public static PostException unauthorized() {
        return new PostException(PostErrorCode.UNAUTHORIZED);
    }

    public static PostException alreadyLiked() {
        return new PostException(PostErrorCode.ALREADY_LIKED);
    }

    public static PostException likeNotFound() {
        return new PostException(PostErrorCode.LIKE_NOT_FOUND);
    }

    public static PostException commentNotFound() {
        return new PostException(PostErrorCode.COMMENT_NOT_FOUND);
    }

    public static PostException commentUnauthorized() {
        return new PostException(PostErrorCode.COMMENT_UNAUTHORIZED);
    }

    public static PostException replyDepthExceeded() {
        return new PostException(PostErrorCode.REPLY_DEPTH_EXCEEDED);
    }

    public static PostException commentAlreadyLiked() {
        return new PostException(PostErrorCode.COMMENT_ALREADY_LIKED);
    }

    public static PostException commentLikeNotFound() {
        return new PostException(PostErrorCode.COMMENT_LIKE_NOT_FOUND);
    }

    public static PostException privatePost() {
        return new PostException(PostErrorCode.POST_PRIVATE);
    }
}
