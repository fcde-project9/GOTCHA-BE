package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.ReviewImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
