package com.gotcha.domain.review.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.review.dto.CreateReviewRequest;
import com.gotcha.domain.review.dto.PageResponse;
import com.gotcha.domain.review.dto.ReviewLikeResponse;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.review.dto.UpdateReviewRequest;
import com.gotcha.domain.review.service.ReviewLikeService;
import com.gotcha.domain.review.service.ReviewService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequestMapping("/api/shops/{shopId}/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;
    private final UserRepository userRepository;

    @Operation(summary = "리뷰 작성", description = "리뷰 작성 (이미지 0~10개)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long shopId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        User currentUser = getCurrentUserOrThrow();
        ReviewResponse response = reviewService.createReview(shopId, currentUser.getId(), request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "리뷰 목록 조회 (무한스크롤)",
            description = "가게의 리뷰 목록을 페이징하여 조회합니다. " +
                    "정렬: LATEST(최신순, 기본값), LIKE_COUNT(좋아요순). " +
                    "무한스크롤 구현: page 파라미터를 증가시키면서 호출, hasNext로 다음 페이지 존재 여부 확인"
    )
    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        PageResponse<ReviewResponse> response = reviewService.getReviews(shopId, sortBy, pageable, currentUserId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰를 수정합니다 (이미지 0~10개)")
    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        User currentUser = getCurrentUserOrThrow();
        ReviewResponse response = reviewService.updateReview(shopId, reviewId, currentUser.getId(), request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다")
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId
    ) {
        User currentUser = getCurrentUserOrThrow();
        reviewService.deleteReview(shopId, reviewId, currentUser.getId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "리뷰 좋아요", description = "리뷰에 좋아요를 추가합니다")
    @PostMapping("/{reviewId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewLikeResponse> addLike(
            @PathVariable Long shopId,
            @PathVariable Long reviewId
    ) {
        return ApiResponse.success(reviewLikeService.addLike(reviewId));
    }

    @Operation(summary = "리뷰 좋아요 취소", description = "리뷰 좋아요를 취소합니다")
    @DeleteMapping("/{reviewId}/like")
    public ApiResponse<ReviewLikeResponse> removeLike(
            @PathVariable Long shopId,
            @PathVariable Long reviewId
    ) {
        return ApiResponse.success(reviewLikeService.removeLike(reviewId));
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

    /**
     * 현재 로그인한 사용자를 가져오거나, 없으면 예외 발생
     * @return 로그인한 사용자
     * @throws IllegalStateException 비로그인 시
     */
    private User getCurrentUserOrThrow() {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다");
        }
        return user;
    }
}
