package com.gotcha.domain.shop.repository;

import com.gotcha.domain.shop.entity.ShopReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopReportRepository extends JpaRepository<ShopReport, Long> {

    /**
     * 가게 삭제 시 해당 가게의 모든 신고 기록 삭제
     * flushAutomatically = true: Shop 삭제 전 신고 기록이 먼저 삭제되도록 즉시 flush
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ShopReport sr WHERE sr.shop.id = :shopId")
    void deleteAllByShopId(@Param("shopId") Long shopId);
}
