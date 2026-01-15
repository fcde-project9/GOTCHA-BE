package com.gotcha.domain.favorite.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.entity.Favorite;
import com.gotcha.domain.favorite.exception.FavoriteException;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final SecurityUtil securityUtil;

    @Transactional
    public FavoriteResponse addFavorite(Long shopId) {
        User currentUser = securityUtil.getCurrentUser();

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(ShopException::notFound);

        if (favoriteRepository.findByUserIdAndShopId(currentUser.getId(), shopId).isPresent()) {
            throw FavoriteException.alreadyFavorited();
        }

        Favorite favorite = Favorite.builder()
                .user(currentUser)
                .shop(shop)
                .build();

        favoriteRepository.save(favorite);

        log.info("Favorite added - userId: {}, shopId: {}", currentUser.getId(), shopId);

        return FavoriteResponse.of(shopId, true);
    }

    @Transactional
    public FavoriteResponse removeFavorite(Long shopId) {
        Long userId = securityUtil.getCurrentUserId();

        if (!shopRepository.existsById(shopId)) {
            throw ShopException.notFound();
        }

        Favorite favorite = favoriteRepository.findByUserIdAndShopId(userId, shopId)
                .orElseThrow(() -> FavoriteException.notFound(shopId));

        favoriteRepository.delete(favorite);

        log.info("Favorite removed - userId: {}, shopId: {}", userId, shopId);

        return FavoriteResponse.of(shopId, false);
    }

    public List<FavoriteShopResponse> getMyFavorites() {
        Long userId = securityUtil.getCurrentUserId();

        // N+1 방지: JOIN FETCH를 사용하여 Shop을 함께 조회 (전체 조회)
        List<Favorite> favorites = favoriteRepository.findAllByUserIdWithShop(userId);

        return favorites.stream()
                .map(favorite -> {
                    Shop shop = favorite.getShop();
                    String openStatus = shopService.getOpenStatus(shop.getOpenTime());
                    return FavoriteShopResponse.from(favorite, openStatus);
                })
                .collect(Collectors.toList());
    }
}
