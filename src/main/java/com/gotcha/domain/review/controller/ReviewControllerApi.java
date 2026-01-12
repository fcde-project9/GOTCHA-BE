package com.gotcha.domain.review.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.review.dto.CreateReviewRequest;
import com.gotcha.domain.review.dto.PageResponse;
import com.gotcha.domain.review.dto.ReviewLikeResponse;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.review.dto.UpdateReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Review", description = "리뷰 API")
public interface ReviewControllerApi {

    @Operation(summary = "리뷰 작성", description = "리뷰 작성 (이미지 0~10개)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "리뷰 작성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (R002: 리뷰 길이 오류, R005: 이미지 개수 초과, C001-C003: 요청 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
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
            )
    })
    ApiResponse<ReviewResponse> createReview(
            @PathVariable Long shopId,
            @Valid @RequestBody CreateReviewRequest request
    );

    @Operation(
            summary = "리뷰 목록 조회 (무한스크롤)",
            description = "가게의 리뷰 목록을 페이징하여 조회합니다. " +
                    "정렬: LATEST(최신순, 기본값), LIKE_COUNT(좋아요순). " +
                    "무한스크롤 구현: page 파라미터를 증가시키면서 호출, hasNext로 다음 페이지 존재 여부 확인"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            )
    })
    ApiResponse<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다 (이미지 0~10개)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "리뷰 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (R002: 리뷰 길이 오류, R005: 이미지 개수 초과, C001-C003: 요청 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 리뷰만 수정 가능 (R003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "리뷰를 찾을 수 없음 (R001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ReviewResponse> updateReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    );

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "리뷰 삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "본인의 리뷰만 삭제 가능 (R003)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "리뷰를 찾을 수 없음 (R001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<Void> deleteReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId
    );

    @Operation(
            summary = "리뷰 좋아요",
            description = "리뷰에 좋아요를 추가합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "좋아요 추가 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "리뷰를 찾을 수 없음 (R001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 좋아요한 리뷰 (R006)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ReviewLikeResponse> addLike(@PathVariable Long reviewId);

    @Operation(
            summary = "리뷰 좋아요 취소",
            description = "리뷰 좋아요를 취소합니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 취소 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요 (A001)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "좋아요 정보를 찾을 수 없음 (R007)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ReviewLikeResponse> removeLike(@PathVariable Long reviewId);
}
