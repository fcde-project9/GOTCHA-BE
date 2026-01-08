package com.gotcha.domain.favorite.service;

import com.gotcha._global.common.PageResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.entity.Favorite;
import com.gotcha.domain.favorite.exception.FavoriteException;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public PageResponse<FavoriteShopResponse> getMyFavorites(Double lat, Double lng, Pageable pageable) {
        Long userId = securityUtil.getCurrentUserId();

        Page<Favorite> favoritePage = favoriteRepository.findAllByUserIdWithShop(userId, pageable);

        List<FavoriteShopResponse> content = favoritePage.getContent().stream()
                .map(favorite -> {
                    Shop shop = favorite.getShop();

                    Integer distance = null;
                    if (lat != null && lng != null) {
                        distance = calculateDistance(lat, lng, shop.getLatitude(), shop.getLongitude());
                    }

                    Boolean isOpen = null;

                    return FavoriteShopResponse.from(favorite, distance, isOpen);
                })
                .collect(Collectors.toList());

        return PageResponse.from(favoritePage, content);
    }

    private Integer calculateDistance(Double lat1, Double lng1, Double lat2, Double lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }

        final int EARTH_RADIUS = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distanceKm = EARTH_RADIUS * c;
        return (int) (distanceKm * 1000);
    }
}
