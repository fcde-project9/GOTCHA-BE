package com.gotcha.domain.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 정렬 방식")
public enum PostSortType {

    /** 최신순 (기본, 커서 기반) */
    LATEST,

    /** 인기순 (최근 7일 좋아요 많은 순, 페이지 기반) */
    POPULAR
}
