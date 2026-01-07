package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.NearbyShopResponse;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shop", description = "가게 API")
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopController {

    private final ShopService shopService;

    @Operation(
            summary = "가게 생성",
            description = "위도/경도를 받아서 카카오맵 API로 주소를 자동으로 변환하여 가게를 생성합니다"
    )
    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopResponse> saveShop(
            @Valid @RequestBody CreateShopRequest request,
            @RequestParam
            @NotNull(message = "위도는 필수입니다")
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
            @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
            Double latitude,
            @RequestParam
            @NotNull(message = "경도는 필수입니다")
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
            @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
            Double longitude
    ) {
        Shop shop = shopService.createShop(
                request.name(),
                latitude,
                longitude,
                request.mainImageUrl(),
                request.locationHint(),
                request.openTime()
        );

        return ApiResponse.success(ShopResponse.from(shop));
    }

    @Operation(
            summary = "가게 저장 전 근처 가게 조회",
            description = "가게 등록 전 현재 위도/경도 기준 50m 이내 가게를 거리 가까운 순서로 조회합니다 (중복 체크용)"
    )
    @GetMapping("/nearby")
    public ApiResponse<List<NearbyShopResponse>> checkNearbyShopsBeforeSave(
            @RequestParam
            @NotNull(message = "위도는 필수입니다")
            @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
            @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
            Double latitude,
            @RequestParam
            @NotNull(message = "경도는 필수입니다")
            @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
            @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
            Double longitude
    ) {
        List<NearbyShopResponse> shops = shopService.checkNearbyShopsBeforeSave(latitude, longitude);
        return ApiResponse.success(shops);
    }
}
