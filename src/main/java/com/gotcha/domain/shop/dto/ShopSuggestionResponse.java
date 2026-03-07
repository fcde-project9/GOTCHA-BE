package com.gotcha.domain.shop.dto;

import com.gotcha.domain.shop.entity.ShopSuggestion;
import com.gotcha.domain.shop.entity.SuggestionReason;
import java.time.LocalDateTime;
import java.util.List;

public record ShopSuggestionResponse(
    Long id,
    Long shopId,
    List<SuggestionReason> reasons,
    LocalDateTime createdAt
) {
    public static ShopSuggestionResponse from(ShopSuggestion suggestion) {
        return new ShopSuggestionResponse(
            suggestion.getId(),
            suggestion.getShop().getId(),
            suggestion.getReasons(),
            suggestion.getCreatedAt()
        );
    }
}
