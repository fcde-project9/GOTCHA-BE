package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
                request.locationHint()
        );

        return ApiResponse.success(ShopResponse.from(shop));
    }
}
