package com.gotcha.domain.shop.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.favorite.dto.FavoriteResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.shop.dto.CoordinateRequest;
import com.gotcha.domain.shop.dto.CreateShopRequest;
import com.gotcha.domain.shop.dto.NearbyShopsResponse;
import com.gotcha.domain.shop.dto.ShopDetailResponse;
import com.gotcha.domain.shop.dto.ShopMapResponse;
import com.gotcha.domain.shop.dto.ShopResponse;
import com.gotcha.domain.shop.dto.ShopSearchResultResponse;
import com.gotcha.domain.shop.dto.UpdateShopMainImageRequest;
import com.gotcha.domain.shop.dto.UpdateShopRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Shop", description = "가게 API")
public interface ShopControllerApi {

    @Operation(
            summary = "가게 생성",
            description = "위도/경도를 받아서 카카오맵 API로 주소를 자동으로 변환하여 가게를 생성합니다. " +
                    "로그인한 사용자는 created_by에 기록되고, 비로그인 사용자는 익명으로 제보됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "가게 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (S003: 가게명 길이 오류, S004: 좌표 오류, C001-C003: 요청 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복된 가게 (S002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ShopResponse> saveShop(
            @Valid @RequestBody CreateShopRequest request,
            @Valid @ModelAttribute CoordinateRequest coordinate
    );

    @Operation(
            summary = "가게 이름/주소 검색",
            description = "가게 이름 또는 주소로 부분 일치 검색합니다. " +
                    "keyword는 1자 이상이어야 하며, 공백만 입력하면 400 오류를 반환합니다. " +
                    "lat/lng를 모두 제공하면 거리순 정렬 후 결과를 반환하고, " +
                    "하나만 제공하면 400 오류를 반환합니다. " +
                    "lat/lng를 제공하지 않으면 이름순으로 정렬됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검색어 없음 (S009) 또는 좌표 오류 (S004)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ShopSearchResultResponse> searchShops(
            @RequestParam String keyword,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "가게 저장 전 근처 가게 조회",
            description = "가게 등록 전 현재 위도/경도 기준 50m 이내 가게를 거리 가까운 순서로 조회합니다 (중복 체크용)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (S004: 좌표 오류, C001-C003: 요청 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<NearbyShopsResponse> checkNearbyShopsBeforeSave(
            @Valid @ModelAttribute CoordinateRequest coordinate
    );

    @Operation(
            summary = "지도 영역 내 가게 목록 조회",
            description = "카카오맵에서 보이는 영역(bounds) 내의 가게를 거리 가까운 순서로 조회합니다. " +
                    "latitude, longitude는 선택 파라미터이며, 사용자 위치 정보가 없으면 distance는 null로 반환됩니다. " +
                    "거리는 10m 단위로 계산되며 (1000m 미만: \"50m\", 1000m 이상: \"1.5km\"), " +
                    "로그인한 사용자는 찜 여부를 확인할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (S004: 좌표 오류, C001-C003: 요청 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<List<ShopMapResponse>> getShopsInMap(
            @RequestParam Double northEastLat,
            @RequestParam Double northEastLng,
            @RequestParam Double southWestLat,
            @RequestParam Double southWestLng,
            @RequestParam(required = false) String latitude,
            @RequestParam(required = false) String longitude
    );

    @Operation(
            summary = "가게 상세 조회",
            description = "가게 ID로 상세 정보를 조회합니다. 로그인한 사용자는 찜 여부를 확인할 수 있습니다. " +
                    "리뷰 5개를 포함하며, 정렬: LATEST(최신순, 기본값), LIKE_COUNT(좋아요순)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (C003: 유효하지 않은 sortBy 값)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음 (S001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ShopDetailResponse> getShopDetail(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy
    );

    @Operation(
            summary = "찜 추가",
            description = "가게를 찜 목록에 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "찜 추가 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음 (S001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 찜한 가게 (F001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<FavoriteResponse> addFavorite(@PathVariable Long shopId);

    @Operation(
            summary = "찜 삭제",
            description = "가게를 찜 목록에서 삭제합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "찜 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 찜만 삭제 가능 (F003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "찜 정보를 찾을 수 없음 (F002)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<FavoriteResponse> removeFavorite(@PathVariable Long shopId);

    @Operation(
            summary = "가게 정보 수정 (ADMIN)",
            description = "가게 정보를 수정합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가게 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (S008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음 (S001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<Void> updateShop(
            @PathVariable Long shopId,
            @Valid @RequestBody UpdateShopRequest request
    );

    @Operation(
            summary = "가게 대표 이미지 수정 (ADMIN)",
            description = "가게의 대표 이미지를 수정합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "이미지 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (S008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음 (S001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<Void> updateShopMainImage(
            @PathVariable Long shopId,
            @Valid @RequestBody UpdateShopMainImageRequest request
    );

    @Operation(
            summary = "가게 삭제 (ADMIN)",
            description = "가게와 연관된 모든 데이터(리뷰, 찜, 이미지)를 삭제합니다. ADMIN 권한이 필요합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "가게 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (S008)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "가게를 찾을 수 없음 (S001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<Void> deleteShop(@PathVariable Long shopId);
}
