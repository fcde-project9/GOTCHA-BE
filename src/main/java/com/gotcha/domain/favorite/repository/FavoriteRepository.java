package com.gotcha.domain.favorite.repository;

import com.gotcha.domain.favorite.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndShopId(Long userId, Long shopId);

    List<Favorite> findAllByUserId(Long userId);

    Long countByShopId(Long shopId);

    void deleteByUserIdAndShopId(Long userId, Long shopId);
}
