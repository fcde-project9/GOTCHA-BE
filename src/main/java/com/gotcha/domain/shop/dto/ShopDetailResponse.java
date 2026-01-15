package com.gotcha.domain.shop.dto;

import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.shop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Schema(description = "가게 상세 조회 응답")
public record ShopDetailResponse(
        @Schema(description = "가게 ID", example = "1")
        Long id,

        @Schema(description = "가게명", example = "가챠샵 신사점")
        String name,

        @Schema(description = "주소", example = "서울시 강남구 신사동 123-45")
        String addressName,

        @Schema(description = "위치 힌트", example = "강남역 2번 출구 내 델리만쥬집 좌측")
        String locationHint,

        @Schema(description = "운영 시간 (JSON 형식)", example =
                "{\"Mon\":\"10:00-22:00\",\"Tue\":null,\"Wed\":\"10:00-22:00\"}")
        String openTime,
        @Schema(description = "오늘 영업 시간 (한국 시간 기준)", example = "10:00-22:00", nullable = true)
        String todayOpenTime,

        @Schema(description = "영업 상태 (한국 시간 기준)", example = "영업 중", allowableValues = {"영업 중", "영업 종료", "휴무", ""})
        String openStatus,

        @Schema(description = "위도", example = "37.517305")
        Double latitude,

        @Schema(description = "경도", example = "127.022775")
        Double longitude,

        @Schema(description = "대표 이미지 URL")
        String mainImageUrl,

        @Schema(description = "찜 여부 (로그인 사용자만)", example = "false")
        Boolean isFavorite,

        @Schema(description = "리뷰 목록 (최대 5개)")
        List<ReviewResponse> reviews,

        @Schema(description = "전체 리뷰 개수", example = "42")
        Long reviewCount,

        @Schema(description = "전체 리뷰 사진 개수", example = "25")
        Long totalReviewImageCount,

        @Schema(description = "최신 리뷰 이미지 4개 (URL 리스트)")
        List<String> recentReviewImages
) {
    /**
     * Shop 엔티티를 ShopDetailResponse로 변환
     *
     * @param shop                   Shop 엔티티
     * @param todayOpenTime          오늘의 영업 시간
     * @param openStatus             영업 상태 ("영업 중", "영업 종료", "휴무", "")
     * @param isFavorite             찜 여부
     * @param reviews                리뷰 목록
     * @param reviewCount            전체 리뷰 개수
     * @param totalReviewImageCount  전체 리뷰 사진 개수
     * @param recentReviewImages     최신 리뷰 이미지 4개
     * @return ShopDetailResponse
     */
    public static ShopDetailResponse of(Shop shop, String todayOpenTime, String openStatus, Boolean isFavorite,
                                        List<ReviewResponse> reviews, Long reviewCount, Long totalReviewImageCount,
                                        List<String> recentReviewImages) {
        Objects.requireNonNull(shop, "Shop must not be null");

        return new ShopDetailResponse(
                shop.getId(),
                shop.getName(),
                shop.getAddressName(),
                shop.getLocationHint(),
                shop.getOpenTime(),
                todayOpenTime,
                openStatus,
                shop.getLatitude(),
                shop.getLongitude(),
                shop.getMainImageUrl(),
                isFavorite,
                reviews != null ? reviews : Collections.emptyList(),
                reviewCount,
                totalReviewImageCount,
                recentReviewImages != null ? recentReviewImages : Collections.emptyList()
        );
    }
}
