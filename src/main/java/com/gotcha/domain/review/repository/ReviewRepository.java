package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    List<Review> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
