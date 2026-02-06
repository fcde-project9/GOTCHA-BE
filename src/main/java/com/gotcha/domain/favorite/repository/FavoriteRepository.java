package com.gotcha.domain.favorite.repository;

import com.gotcha.domain.favorite.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndShopId(Long userId, Long shopId);

    boolean existsByUserIdAndShopId(Long userId, Long shopId);

    List<Favorite> findAllByUserId(Long userId);

    Long countByShopId(Long shopId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId AND f.shop.id = :shopId")
    void deleteByUserIdAndShopId(@Param("userId") Long userId, @Param("shopId") Long shopId);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.shop WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    Page<Favorite> findAllByUserIdWithShop(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.shop WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findAllByUserIdWithShop(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Favorite f WHERE f.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 가게 삭제 시 해당 가게의 모든 찜 삭제
     * flushAutomatically = true: Shop 삭제 전 찜이 먼저 삭제되도록 즉시 flush
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Favorite f WHERE f.shop.id = :shopId")
    void deleteAllByShopId(@Param("shopId") Long shopId);
}
