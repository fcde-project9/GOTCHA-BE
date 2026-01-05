package com.gotcha.domain.review.repository;

import com.gotcha.domain.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    boolean existsByUserIdAndShopId(Long userId, Long shopId);
}
