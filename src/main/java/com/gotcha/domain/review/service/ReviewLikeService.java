package com.gotcha.domain.review.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.review.dto.ReviewLikeResponse;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewLike;
import com.gotcha.domain.review.exception.ReviewException;
import com.gotcha.domain.review.repository.ReviewLikeRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewLikeService {

    private final ReviewLikeRepository reviewLikeRepository;
    private final ReviewRepository reviewRepository;
    private final SecurityUtil securityUtil;

    @Transactional
    public ReviewLikeResponse addLike(Long reviewId) {
        User currentUser = securityUtil.getCurrentUser();

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        if (reviewLikeRepository.findByUserIdAndReviewId(currentUser.getId(), reviewId).isPresent()) {
            throw ReviewException.alreadyLiked();
        }

        ReviewLike reviewLike = ReviewLike.builder()
                .user(currentUser)
                .review(review)
                .build();

        reviewLikeRepository.save(reviewLike);

        log.info("Review like added - userId: {}, reviewId: {}", currentUser.getId(), reviewId);

        return ReviewLikeResponse.of(reviewId, true);
    }

    @Transactional
    public ReviewLikeResponse removeLike(Long reviewId) {
        User currentUser = securityUtil.getCurrentUser();

        // 1. 리뷰 존재 확인
        if (!reviewRepository.existsById(reviewId)) {
            throw ReviewException.notFound(reviewId);
        }

        // 2. 현재 사용자가 이 리뷰에 좋아요를 했는지 확인 (본인의 좋아요만 취소 가능)
        ReviewLike reviewLike = reviewLikeRepository.findByUserIdAndReviewId(currentUser.getId(), reviewId)
                .orElseThrow(() -> {
                    log.warn("User {} attempted to remove like from review {} but never liked it",
                            currentUser.getId(), reviewId);
                    return ReviewException.likeNotFound(reviewId);
                });

        // 3. 좋아요 삭제
        reviewLikeRepository.delete(reviewLike);

        log.info("Review like removed - userId: {}, reviewId: {}", currentUser.getId(), reviewId);

        return ReviewLikeResponse.of(reviewId, false);
    }
}
