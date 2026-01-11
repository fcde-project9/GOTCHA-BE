package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    // Review별 이미지 조회 (순서대로)
    List<ReviewImage> findAllByReviewIdOrderByDisplayOrder(Long reviewId);

    // Review별 이미지 조회 (In 쿼리 - N+1 방지)
    List<ReviewImage> findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(List<Long> reviewIds);

    // Review 삭제 시 연관 이미지 삭제
    void deleteAllByReviewId(Long reviewId);

    // 이미지 개수 카운트
    int countByReviewId(Long reviewId);

    // 여러 Review의 이미지 일괄 삭제
    void deleteAllByReviewIdIn(List<Long> reviewIds);

    // 특정 가게의 전체 리뷰 이미지 개수
    @Query("SELECT COUNT(ri) FROM ReviewImage ri WHERE ri.review.shop.id = :shopId")
    Long countByShopId(@Param("shopId") Long shopId);

    // 특정 가게의 최신 리뷰 이미지 4개 (리뷰 생성일시 기준 내림차순)
    @Query(value = "SELECT ri.* FROM review_images ri " +
            "JOIN reviews r ON ri.review_id = r.id " +
            "WHERE r.shop_id = :shopId " +
            "ORDER BY r.created_at DESC, ri.display_order ASC " +
            "LIMIT 4", nativeQuery = true)
    List<ReviewImage> findTop4ByShopId(@Param("shopId") Long shopId);
}
