package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.ReviewLike;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    Optional<ReviewLike> findByUserIdAndReviewId(Long userId, Long reviewId);

    Long countByReviewId(Long reviewId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ReviewLike rl WHERE rl.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndReviewId(Long currentUserId, Long reviewId);

    /**
     * 여러 리뷰의 좋아요 수를 일괄 조회 (N+1 방지)
     * @param reviewIds 조회할 리뷰 ID 목록
     * @return reviewId -> likeCount 맵
     */
    @Query("SELECT rl.review.id AS reviewId, COUNT(rl) AS likeCount " +
           "FROM ReviewLike rl " +
           "WHERE rl.review.id IN :reviewIds " +
           "GROUP BY rl.review.id")
    List<ReviewLikeCount> countByReviewIdInGroupByReviewId(@Param("reviewIds") List<Long> reviewIds);

    /**
     * 특정 사용자가 좋아요한 리뷰 ID 목록을 일괄 조회 (N+1 방지)
     * @param userId 사용자 ID
     * @param reviewIds 조회할 리뷰 ID 목록
     * @return 사용자가 좋아요한 리뷰 ID 목록
     */
    @Query("SELECT rl.review.id " +
           "FROM ReviewLike rl " +
           "WHERE rl.user.id = :userId AND rl.review.id IN :reviewIds")
    List<Long> findLikedReviewIds(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);

    /**
     * 좋아요 카운트 결과를 담는 Projection 인터페이스
     */
    interface ReviewLikeCount {
        Long getReviewId();
        Long getLikeCount();
    }
}
