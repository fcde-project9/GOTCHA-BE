package com.gotcha.domain.user.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.common.PageResponse;
import com.gotcha.domain.favorite.dto.FavoriteShopResponse;
import com.gotcha.domain.favorite.service.FavoriteService;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FavoriteService favoriteService;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 정보를 조회합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @Operation(
            summary = "내 찜 목록 조회",
            description = "현재 로그인한 사용자의 찜 목록을 조회합니다. lat, lng 파라미터를 제공하면 거리가 계산됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/me/favorites")
    public ApiResponse<PageResponse<FavoriteShopResponse>> getMyFavorites(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponse.success(favoriteService.getMyFavorites(lat, lng, pageable));
    }
}
