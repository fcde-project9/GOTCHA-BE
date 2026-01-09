package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.ReviewLike;
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
}
