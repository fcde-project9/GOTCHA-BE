package com.gotcha.domain.review.controller;

import com.gotcha._global.common.ApiResponse;
import com.gotcha.domain.review.dto.CreateReviewRequest;
import com.gotcha.domain.review.dto.PageResponse;
import com.gotcha.domain.review.dto.ReviewImageListResponse;
import com.gotcha.domain.review.dto.ReviewLikeResponse;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.review.dto.UpdateReviewRequest;
import com.gotcha.domain.review.service.ReviewLikeService;
import com.gotcha.domain.review.service.ReviewService;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
@Validated
public class ReviewController implements ReviewControllerApi {

    private final ReviewService reviewService;
    private final ReviewLikeService reviewLikeService;
    private final UserRepository userRepository;

    @Override
    @PostMapping("/{shopId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> createReview(
            @PathVariable Long shopId,
            @Valid @RequestBody CreateReviewRequest request
    ) {
        User currentUser = getCurrentUserOrThrow();
        ReviewResponse response = reviewService.createReview(shopId, currentUser.getId(), request);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/{shopId}/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> getReviews(
            @PathVariable Long shopId,
            @RequestParam(defaultValue = "LATEST") ReviewSortType sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        User currentUser = getCurrentUser();
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        PageResponse<ReviewResponse> response = reviewService.getReviews(shopId, sortBy, pageable, currentUserId);
        return ApiResponse.success(response);
    }

    @Override
    @PutMapping("/{shopId}/reviews/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request
    ) {
        User currentUser = getCurrentUserOrThrow();
        ReviewResponse response = reviewService.updateReview(shopId, reviewId, currentUser.getId(), request);
        return ApiResponse.success(response);
    }

    @Override
    @DeleteMapping("/{shopId}/reviews/{reviewId}")
    public ApiResponse<Void> deleteReview(
            @PathVariable Long shopId,
            @PathVariable Long reviewId
    ) {
        User currentUser = getCurrentUserOrThrow();
        reviewService.deleteReview(shopId, reviewId, currentUser.getId());
        return ApiResponse.success(null);
    }

    @Override
    @PostMapping("/reviews/{reviewId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewLikeResponse> addLike(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewLikeService.addLike(reviewId));
    }

    @Override
    @DeleteMapping("/reviews/{reviewId}/like")
    public ApiResponse<ReviewLikeResponse> removeLike(@PathVariable Long reviewId) {
        return ApiResponse.success(reviewLikeService.removeLike(reviewId));
    }

    @Override
    @GetMapping("/{shopId}/reviews/images")
    public ApiResponse<ReviewImageListResponse> getShopReviewImages(@PathVariable Long shopId) {
        ReviewImageListResponse response = reviewService.getShopReviewImages(shopId);
        return ApiResponse.success(response);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return userRepository.findById(userId).orElse(null);
        }

        return null;
    }

    private User getCurrentUserOrThrow() {
        User user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("로그인이 필요합니다");
        }
        return user;
    }
}
