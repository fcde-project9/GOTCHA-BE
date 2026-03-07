package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.shop.dto.CreateShopSuggestionRequest;
import com.gotcha.domain.shop.dto.ShopSuggestionResponse;
import com.gotcha.domain.shop.service.ShopSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Shop Suggestion", description = "가게 정보 수정 제안 API")
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ShopSuggestionController {

    private final ShopSuggestionService shopSuggestionService;

    @Operation(summary = "가게 정보 수정 제안", description = "복수 선택 가능. 로그인 필요.")
    @PostMapping("/{shopId}/suggest")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ShopSuggestionResponse> createSuggestion(
            @PathVariable Long shopId,
            @Valid @RequestBody CreateShopSuggestionRequest request
    ) {
        ShopSuggestionResponse response = shopSuggestionService.createSuggestion(shopId, request);
        return ApiResponse.success(response);
    }
}
