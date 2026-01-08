package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.shop.dto.CoordinateRequest;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.MapBoundsRequest;
import com.gotcha.domain.shop.dto.NearbyShopsResponse;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shop", description = "가게 API")
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;
    private final UserRepository userRepository;
    private final FavoriteService favoriteService;

    @Operation(
            summary = "가게 생성",
            description = "위도/경도를 받아서 카카오맵 API로 주소를 자동으로 변환하여 가게를 생성합니다. 로그인한 사용자는 created_by에 기록되고, 비로그인 사용자는 익명으로 제보됩니다."
    )
    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopResponse> saveShop(
            @Valid @RequestBody CreateShopRequest request,
            @Valid @ModelAttribute CoordinateRequest coordinate
    ) {
        // 현재 로그인한 사용자 가져오기 (비로그인 시 null)
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

    @Operation(
            summary = "가게 저장 전 근처 가게 조회",
            description = "가게 등록 전 현재 위도/경도 기준 50m 이내 가게를 거리 가까운 순서로 조회합니다 (중복 체크용)"
    )
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

    @Operation(
            summary = "지도 영역 내 가게 목록 조회",
            description = "카카오맵에서 보이는 영역(bounds) 내의 가게를 거리 가까운 순서로 조회합니다. " +
                    "거리는 50m 단위로 표시되며, 로그인한 사용자는 찜 여부를 확인할 수 있습니다."
    )
    @GetMapping("/map")
    public ApiResponse<List<ShopMapResponse>> getShopsInMap(
            @Valid @ModelAttribute MapBoundsRequest bounds
    ) {
        User user = getCurrentUser();
        List<ShopMapResponse> shops = shopService.getShopsInMap(
                bounds.northEastLat(),
                bounds.northEastLng(),
                bounds.southWestLat(),
                bounds.southWestLng(),
                bounds.centerLat(),
                bounds.centerLng(),
                user
        );

        return ApiResponse.success(shops);
    }


    @Operation(
            summary = "찜 추가",
            description = "가게를 찜 목록에 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{shopId}/favorite")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> addFavorite(@PathVariable Long shopId) {
        return ApiResponse.success(favoriteService.addFavorite(shopId));
    }

    @Operation(
            summary = "찜 삭제",
            description = "가게를 찜 목록에서 삭제합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{shopId}/favorite")
    public ApiResponse<FavoriteResponse> removeFavorite(@PathVariable Long shopId) {
        return ApiResponse.success(favoriteService.removeFavorite(shopId));
    }

    /**
     * SecurityContext에서 현재 로그인한 사용자 정보 가져오기
     * @return 로그인한 사용자 (User) 또는 비로그인 시 null
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 익명 사용자인 경우
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        // JWT 필터에서 userId(Long)를 principal로 설정함
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userRepository.findById(userId).orElse(null);
        }

        return null;
    }
}
