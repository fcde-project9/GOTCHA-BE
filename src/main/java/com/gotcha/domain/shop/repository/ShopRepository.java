package com.gotcha.domain.shop.repository;

import com.gotcha.domain.shop.entity.Shop;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopRepository extends JpaRepository<Shop, Long> {

    @Query("SELECT s FROM Shop s LEFT JOIN FETCH s.createdBy WHERE s.id = :id")
    Optional<Shop> findByIdWithCreator(@Param("id") Long id);

    @Query("SELECT s FROM Shop s WHERE "
            + "(6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * "
            + "cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * "
            + "sin(radians(s.latitude)))) < :radius "
            + "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(s.latitude)) * "
            + "cos(radians(s.longitude) - radians(:lng)) + sin(radians(:lat)) * "
            + "sin(radians(s.latitude))))")
    List<Shop> findNearbyShops(@Param("lat") Double lat, @Param("lng") Double lng,
                               @Param("radius") Double radius);

    @Query("SELECT s FROM Shop s WHERE "
            + "s.latitude BETWEEN :southWestLat AND :northEastLat "
            + "AND s.longitude BETWEEN :southWestLng AND :northEastLng")
    List<Shop> findShopsWithinBounds(
            @Param("northEastLat") Double northEastLat,
            @Param("northEastLng") Double northEastLng,
            @Param("southWestLat") Double southWestLat,
            @Param("southWestLng") Double southWestLng
    );

    @Query("SELECT s FROM Shop s JOIN FETCH s.createdBy WHERE s.createdBy.id = :userId ORDER BY s.createdAt DESC")
    Page<Shop> findAllByCreatedByIdWithUser(@Param("userId") Long userId, Pageable pageable);
}
