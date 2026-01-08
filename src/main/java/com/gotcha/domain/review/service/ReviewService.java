package com.gotcha.domain.review.service;

import com.gotcha.domain.file.service.FileUploadService;
import com.gotcha.domain.review.dto.CreateReviewRequest;
import com.gotcha.domain.review.dto.PageResponse;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.UpdateReviewRequest;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.exception.ReviewException;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;

    private static final int MAX_IMAGES = 10;

    @Transactional
    public ReviewResponse createReview(Long shopId, Long userId, CreateReviewRequest request) {
        log.info("Creating review for shop {} by user {}", shopId, userId);

        // 1. Shop 존재 확인
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> ShopException.notFound(shopId));

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        // 3. 이미지 개수 검증 (0~10개)
        validateImageCount(request.imageUrls());

        // 4. Review 엔티티 생성 및 저장
        Review review = Review.builder()
                .shop(shop)
                .user(user)
                .content(request.content())
                .build();
        reviewRepository.save(review);

        log.info("Review created with ID: {}", review.getId());

        // 5. ReviewImage 엔티티 생성 및 저장
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            saveReviewImages(review, request.imageUrls());
            log.info("Saved {} images for review {}", request.imageUrls().size(), review.getId());
        }

        // 6. Response 생성 (이미지 포함)
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(review.getId());
        return ReviewResponse.from(review, user, images, true);
    }

    public PageResponse<ReviewResponse> getReviews(Long shopId, Pageable pageable, Long currentUserId) {
        log.info("Getting reviews for shop {} (page: {})", shopId, pageable.getPageNumber());

        Page<Review> reviewPage = reviewRepository
                .findAllByShopIdOrderByCreatedAtDesc(shopId, pageable);

        // N+1 방지: 이미지 일괄 조회
        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<ReviewImage>> imageMap = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getReview().getId()));

        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(review -> ReviewResponse.from(
                        review,
                        review.getUser(),
                        imageMap.getOrDefault(review.getId(), List.of()),
                        currentUserId != null && review.getUser().getId().equals(currentUserId)
                ))
                .toList();

        return PageResponse.from(reviewPage, responses);
    }

    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, UpdateReviewRequest request) {
        log.info("Updating review {} by user {}", reviewId, userId);

        // 1. Review 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        // 2. 권한 확인
        if (!review.getUser().getId().equals(userId)) {
            log.warn("Unauthorized update attempt for review {} by user {}", reviewId, userId);
            throw ReviewException.unauthorized();
        }

        // 3. 이미지 개수 검증
        validateImageCount(request.imageUrls());

        // 4. Content 수정
        review.updateContent(request.content());

        // 5. 기존 이미지 삭제 (GCS + DB)
        deleteReviewImages(reviewId);

        // 6. 새 이미지 저장
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            saveReviewImages(review, request.imageUrls());
            log.info("Updated {} images for review {}", request.imageUrls().size(), reviewId);
        }

        // 7. Response 생성
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(reviewId);

        log.info("Review {} updated successfully", reviewId);
        return ReviewResponse.from(review, review.getUser(), images, true);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        // 권한 확인
        if (!review.getUser().getId().equals(userId)) {
            log.warn("Unauthorized delete attempt for review {} by user {}", reviewId, userId);
            throw ReviewException.unauthorized();
        }

        // 이미지 파일 삭제 (GCS + DB)
        deleteReviewImages(reviewId);

        // DB 삭제
        reviewRepository.delete(review);

        log.info("Review {} deleted successfully", reviewId);
    }

    private void deleteReviewImages(Long reviewId) {
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(reviewId);

        for (ReviewImage image : images) {
            try {
                fileUploadService.deleteFile(image.getImageUrl());
                log.info("Deleted image file: {}", image.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete image file: {}", image.getImageUrl(), e);
                // 파일 삭제 실패해도 DB는 삭제 진행 (이미 삭제된 파일일 수 있음)
            }
        }

        reviewImageRepository.deleteAllByReviewId(reviewId);
    }

    private void saveReviewImages(Review review, List<String> imageUrls) {
        for (int i = 0; i < imageUrls.size(); i++) {
            ReviewImage image = ReviewImage.builder()
                    .review(review)
                    .imageUrl(imageUrls.get(i))
                    .displayOrder(i)
                    .build();
            reviewImageRepository.save(image);
        }
    }

    private void validateImageCount(List<String> imageUrls) {
        if (imageUrls != null && imageUrls.size() > MAX_IMAGES) {
            log.warn("Too many images provided: {}", imageUrls.size());
            throw ReviewException.tooManyImages(imageUrls.size());
        }
    }
}
