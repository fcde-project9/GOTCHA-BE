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

    @Query("SELECT r FROM Review r LEFT JOIN ReviewLike rl ON r.id = rl.review.id " +
            "WHERE r.shop.id = :shopId " +
            "GROUP BY r.id " +
            "ORDER BY COUNT(rl.id) DESC, r.createdAt DESC")
    Page<Review> findAllByShopIdOrderByLikeCountDesc(@Param("shopId") Long shopId, Pageable pageable);

    @Query("SELECT r FROM Review r WHERE r.shop.id = :shopId " +
            "AND r.user.id NOT IN :blockedUserIds " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findAllByShopIdExcludingBlockedUsersOrderByCreatedAtDesc(
            @Param("shopId") Long shopId,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    @Query("SELECT r FROM Review r LEFT JOIN ReviewLike rl ON r.id = rl.review.id " +
            "WHERE r.shop.id = :shopId " +
            "AND r.user.id NOT IN :blockedUserIds " +
            "GROUP BY r.id " +
            "ORDER BY COUNT(rl.id) DESC, r.createdAt DESC")
    Page<Review> findAllByShopIdExcludingBlockedUsersOrderByLikeCountDesc(
            @Param("shopId") Long shopId,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    List<Review> findAllByUserId(Long userId);

    List<Review> findAllByShopId(Long shopId);

    Long countByShopId(Long shopId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.shop.id = :shopId " +
            "AND r.user.id NOT IN :blockedUserIds")
    Long countByShopIdExcludingBlockedUsers(
            @Param("shopId") Long shopId,
            @Param("blockedUserIds") List<Long> blockedUserIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 가게 삭제 시 해당 가게의 모든 리뷰 삭제
     * flushAutomatically = true: Shop 삭제 전 리뷰가 먼저 삭제되도록 즉시 flush
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Review r WHERE r.shop.id = :shopId")
    void deleteAllByShopId(@Param("shopId") Long shopId);
}
