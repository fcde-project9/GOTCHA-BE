package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shop", description = "가게 API")
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @Operation(
            summary = "가게 생성",
            description = "위도/경도를 받아서 카카오맵 API로 주소를 자동으로 변환하여 가게를 생성합니다"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopResponse> saveShop(@Valid @RequestBody CreateShopRequest request) {
        Shop shop = shopService.createShop(
                request.name(),
                request.latitude(),
                request.longitude(),
                request.mainImageUrl(),
                request.locationHint(),
                request.openTime()
        );

        return ApiResponse.success(ShopResponse.from(shop));
    }
}
