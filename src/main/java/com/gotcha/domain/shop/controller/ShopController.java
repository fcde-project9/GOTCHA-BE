package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.exception.BusinessException;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.shop.dto.CoordinateRequest;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.MapBoundsRequest;
import com.gotcha.domain.shop.dto.NearbyShopsResponse;
import com.gotcha.domain.shop.dto.ShopDetailResponse;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopErrorCode;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가게 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (S003: 가게명 길이 오류, S004: 좌표 오류, C001-C003: 요청 형식 오류)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 가게 (S002)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (S004: 좌표 오류, C001-C003: 요청 형식 오류)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (S004: 좌표 오류, C001-C003: 요청 형식 오류)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/map")
    public ApiResponse<List<ShopMapResponse>> getShopsInMap(
            @Valid @ModelAttribute MapBoundsRequest bounds
    ) {
        // 사용자 위치 좌표 필수 체크 (거리 계산에 필수)
        if (bounds.latitude() == null || bounds.longitude() == null) {
            throw new BusinessException(ShopErrorCode.INVALID_COORDINATES, "사용자 위치 좌표(latitude, longitude)는 필수입니다");
        }

        User user = getCurrentUser();
        List<ShopMapResponse> shops = shopService.getShopsInMap(
                bounds.northEastLat(),
                bounds.northEastLng(),
                bounds.southWestLat(),
                bounds.southWestLng(),
                bounds.latitude(),
                bounds.longitude(),
                user
        );

        return ApiResponse.success(shops);
    }

    @Operation(
            summary = "가게 상세 조회",
            description = "가게 ID로 상세 정보를 조회합니다. 로그인한 사용자는 찜 여부를 확인할 수 있습니다. " +
                    "리뷰 5개를 포함하며, 정렬: LATEST(최신순, 기본값), LIKE_COUNT(좋아요순)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (C003: 유효하지 않은 sortBy 값)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가게를 찾을 수 없음 (S001)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{shopId}")
    public ApiResponse<ShopDetailResponse> getShopDetail(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy
    ) {
        User user = getCurrentUser();
        ShopDetailResponse response = shopService.getShopDetail(shopId, sortBy, user);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "찜 추가",
            description = "가게를 찜 목록에 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "찜 추가 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요 (A001)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "가게를 찾을 수 없음 (S001)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 찜한 가게 (F001)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "찜 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요 (A001)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인의 찜만 삭제 가능 (F003)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "찜 정보를 찾을 수 없음 (F002)", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
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
