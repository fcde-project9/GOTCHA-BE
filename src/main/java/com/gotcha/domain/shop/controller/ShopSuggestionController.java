package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.shop.dto.CreateShopSuggestionRequest;
import com.gotcha.domain.shop.dto.ShopSuggestReasonResponse;
import com.gotcha.domain.shop.dto.ShopSuggestionResponse;
import com.gotcha.domain.shop.service.ShopSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "가게 정보 수정 제안 사유 목록 조회", description = "프론트엔드 선택지 구성용. 로그인 불필요.")
    @GetMapping("/suggest-reasons")
    public ApiResponse<List<ShopSuggestReasonResponse>> getSuggestReasons() {
        return ApiResponse.success(ShopSuggestReasonResponse.getAllReasons());
    }

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
