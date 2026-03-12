package com.gotcha.domain.shop.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.shop.dto.CreateShopSuggestionRequest;
import com.gotcha.domain.shop.dto.ShopSuggestionResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.entity.ShopSuggestion;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.shop.repository.ShopSuggestionRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopSuggestionService {

    private final ShopSuggestionRepository shopSuggestionRepository;
    private final ShopRepository shopRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public ShopSuggestionResponse createSuggestion(Long shopId, CreateShopSuggestionRequest request) {
        User currentUser = securityUtil.getCurrentUser();

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> ShopException.notFound(shopId));

        ShopSuggestion suggestion = ShopSuggestion.builder()
                .shop(shop)
                .suggester(currentUser)
                .reasons(request.reasons())
                .build();

        ShopSuggestion saved = shopSuggestionRepository.save(suggestion);
        log.info("ShopSuggestion created - shopId: {}, userId: {}, reasons: {}",
                shopId, currentUser.getId(), request.reasons());

        return ShopSuggestionResponse.from(saved);
    }
}
