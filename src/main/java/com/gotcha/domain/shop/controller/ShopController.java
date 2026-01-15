package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.shop.dto.CoordinateRequest;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.NearbyShopsResponse;
import com.gotcha.domain.shop.dto.ShopDetailResponse;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopController implements ShopControllerApi {

    private final ShopService shopService;
    private final UserRepository userRepository;
    private final FavoriteService favoriteService;

    @Override
    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopResponse> saveShop(
            @Valid @RequestBody CreateShopRequest request,
            @Valid @ModelAttribute CoordinateRequest coordinate
    ) {
        User currentUser = getCurrentUser();

        Shop shop = shopService.createShop(
                request.name(),
                coordinate.latitude(),
                coordinate.longitude(),
                request.mainImageUrl(),
                request.locationHint(),
                request.openTime(),
                currentUser
        );

        return ApiResponse.success(ShopResponse.from(shop));
    }

    @Override
    @GetMapping("/nearby")
    public ApiResponse<NearbyShopsResponse> checkNearbyShopsBeforeSave(
            @Valid @ModelAttribute CoordinateRequest coordinate
    ) {
        NearbyShopsResponse response = shopService.checkNearbyShopsBeforeSave(
                coordinate.latitude(),
                coordinate.longitude()
        );
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/map")
    public ApiResponse<List<ShopMapResponse>> getShopsInMap(
            @RequestParam Double northEastLat,
            @RequestParam Double northEastLng,
            @RequestParam Double southWestLat,
            @RequestParam Double southWestLng,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude
    ) {
        User user = getCurrentUser();

        Double lat = parseDoubleOrNull(latitude);
        Double lng = parseDoubleOrNull(longitude);

        List<ShopMapResponse> shops = shopService.getShopsInMap(
                northEastLat,
                northEastLng,
                southWestLat,
                southWestLng,
                lat,
                lng,
                user
        );

        return ApiResponse.success(shops);
    }

    @Override
    @GetMapping("/{shopId}")
    public ApiResponse<ShopDetailResponse> getShopDetail(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy
    ) {
        User user = getCurrentUser();
        ShopDetailResponse response = shopService.getShopDetail(shopId, sortBy, user);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/{shopId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> addFavorite(@PathVariable Long shopId) {
        return ApiResponse.success(favoriteService.addFavorite(shopId));
    }

    @Override
    @DeleteMapping("/{shopId}/favorite")
    public ApiResponse<FavoriteResponse> removeFavorite(@PathVariable Long shopId) {
        return ApiResponse.success(favoriteService.removeFavorite(shopId));
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userRepository.findById(userId).orElse(null);
        }

        return null;
    }
}
