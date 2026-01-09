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
        Long userId = securityUtil.getCurrentUserId();

        if (!reviewRepository.existsById(reviewId)) {
            throw ReviewException.notFound(reviewId);
        }

        ReviewLike reviewLike = reviewLikeRepository.findByUserIdAndReviewId(userId, reviewId)
                .orElseThrow(() -> ReviewException.likeNotFound(reviewId));

        reviewLikeRepository.delete(reviewLike);

        log.info("Review like removed - userId: {}, reviewId: {}", userId, reviewId);

        return ReviewLikeResponse.of(reviewId, false);
    }
}
