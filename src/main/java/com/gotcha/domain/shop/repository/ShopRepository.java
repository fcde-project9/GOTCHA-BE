package com.gotcha.domain.shop.repository;

import com.gotcha.domain.shop.entity.Shop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.createdBy WHERE s.id = :id")
    Optional<Shop> findByIdWithCreator(@Param("id") Long id);

    @Query("SELECT s FROM Shop s WHERE "
            + "(6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * "
            + "cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * "
            + "sin(radians(s.latitude)))) < :radius")
    List<Shop> findNearbyShops(@Param("lat") Double lat, @Param("lng") Double lng,
                               @Param("radius") Double radius);
}
