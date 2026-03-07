package com.gotcha.domain.shop.repository;

import com.gotcha.domain.shop.entity.ShopSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopSuggestionRepository extends JpaRepository<ShopSuggestion, Long> {

    /**
     * 가게 삭제 시 해당 가게의 모든 제안 기록 삭제
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ShopSuggestion s WHERE s.shop.id = :shopId")
    void deleteAllByShopId(@Param("shopId") Long shopId);

    /**
     * 회원 탈퇴 시 해당 사용자의 모든 제안 기록 삭제
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ShopSuggestion s WHERE s.suggester.id = :userId")
    void deleteBySuggesterId(@Param("userId") Long userId);
}
