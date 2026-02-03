package com.gotcha.domain.review.service;

import com.gotcha.domain.file.service.FileStorageService;
import com.gotcha.domain.review.dto.CreateReviewRequest;
import com.gotcha.domain.review.dto.PageResponse;
import com.gotcha.domain.review.dto.ReviewImageListResponse;
import com.gotcha.domain.review.dto.ReviewResponse;
import com.gotcha.domain.review.dto.ReviewSortType;
import com.gotcha.domain.review.dto.UpdateReviewRequest;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.exception.ReviewException;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewLikeRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.exception.ShopException;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final ReviewLikeRepository reviewLikeRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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
        Long likeCount = reviewLikeRepository.countByReviewId(review.getId());
        // 생성 직후이므로 좋아요는 false
        return ReviewResponse.from(review, user, images, true, likeCount, false);
    }

    public PageResponse<ReviewResponse> getReviews(Long shopId, ReviewSortType sortBy, Pageable pageable, Long currentUserId) {
        log.info("Getting reviews for shop {} (page: {}, sortBy: {})", shopId, pageable.getPageNumber(), sortBy);

        // 정렬 타입에 따라 다른 쿼리 호출
        Page<Review> reviewPage;
        if (sortBy == ReviewSortType.LIKE_COUNT) {
            reviewPage = reviewRepository.findAllByShopIdOrderByLikeCountDesc(shopId, pageable);
        } else {
            // 기본값: 최신순
            reviewPage = reviewRepository.findAllByShopIdOrderByCreatedAtDesc(shopId, pageable);
        }

        // N+1 방지: 이미지 일괄 조회
        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<ReviewImage>> imageMap = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(img -> img.getReview().getId()));

        // N+1 방지: 좋아요 수 일괄 조회 (배치 쿼리)
        Map<Long, Long> likeCountMap = reviewIds.isEmpty()
                ? Map.of()
                : reviewLikeRepository.countByReviewIdInGroupByReviewId(reviewIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ReviewLikeRepository.ReviewLikeCount::getReviewId,
                                ReviewLikeRepository.ReviewLikeCount::getLikeCount
                        ));

        // N+1 방지: 현재 사용자가 좋아요한 리뷰 목록 조회 (배치 쿼리)
        Set<Long> likedReviewIds = Set.of();
        if (currentUserId != null && !reviewIds.isEmpty()) {
            likedReviewIds = new java.util.HashSet<>(
                    reviewLikeRepository.findLikedReviewIds(currentUserId, reviewIds)
            );
        }
        final Set<Long> finalLikedReviewIds = likedReviewIds;

        List<ReviewResponse> responses = reviewPage.getContent().stream()
                .map(review -> ReviewResponse.from(
                        review,
                        review.getUser(),
                        imageMap.getOrDefault(review.getId(), List.of()),
                        review.getUser().getId().equals(currentUserId),
                        likeCountMap.getOrDefault(review.getId(), 0L),
                        finalLikedReviewIds.contains(review.getId())
                ))
                .toList();

        return PageResponse.from(reviewPage, responses);
    }

    @Transactional
    public ReviewResponse updateReview(Long shopId, Long reviewId, User currentUser, UpdateReviewRequest request) {
        log.info("Updating review {} for shop {} by user {}", reviewId, shopId, currentUser.getId());

        // 1. Review 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        // 2. shopId 검증 (보안: 다른 shop의 리뷰 수정 방지)
        if (!review.getShop().getId().equals(shopId)) {
            log.warn("Review {} does not belong to shop {} (actual: {})",
                    reviewId, shopId, review.getShop().getId());
            throw ReviewException.notFound(reviewId);
        }

        // 3. 리뷰 작성자 Lazy 프록시 초기화 (deleteAllByReviewId의 clearAutomatically로 인한 detach 방지)
        User reviewAuthor = review.getUser();
        reviewAuthor.getNickname();

        // 4. 권한 확인 (본인 또는 ADMIN)
        if (!reviewAuthor.getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            log.warn("Unauthorized update attempt for review {} by user {}", reviewId, currentUser.getId());
            throw ReviewException.unauthorized();
        }

        // 5. 이미지 개수 검증
        validateImageCount(request.imageUrls());

        // 5. Content 수정
        review.updateContent(request.content());

        // 6. 기존 이미지 조회 및 삭제할 이미지 필터링
        List<ReviewImage> existingImages = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(reviewId);

        // 새 요청에 포함되지 않은 이미지만 클라우드 스토리지에서 삭제
        List<String> newImageUrls = request.imageUrls() != null
                ? request.imageUrls()
                : List.of();

        for (ReviewImage existingImage : existingImages) {
            if (!newImageUrls.contains(existingImage.getImageUrl())) {
                // 새 요청에 없는 이미지만 클라우드 스토리지에서 삭제
                try {
                    fileStorageService.deleteFile(existingImage.getImageUrl());
                    log.info("Deleted removed image from 클라우드 스토리지: {}", existingImage.getImageUrl());
                } catch (Exception e) {
                    log.error("Failed to delete image file: {}", existingImage.getImageUrl(), e);
                    // 파일 삭제 실패해도 DB는 삭제 진행
                }
            }
        }

        // 7. DB 이미지 전체 삭제 (클라우드 스토리지는 위에서 필요한 것만 삭제함)
        reviewImageRepository.deleteAllByReviewId(reviewId);

        // 8. 새 이미지 저장 (순서대로 displayOrder 할당)
        if (!newImageUrls.isEmpty()) {
            saveReviewImages(review, newImageUrls);
            log.info("Updated {} images for review {}", newImageUrls.size(), reviewId);
        }

        // 9. Response 생성
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(reviewId);
        Long likeCount = reviewLikeRepository.countByReviewId(reviewId);
        boolean isOwner = reviewAuthor.getId().equals(currentUser.getId());
        boolean isLiked = reviewLikeRepository.existsByUserIdAndReviewId(currentUser.getId(), reviewId);

        log.info("Review {} updated successfully", reviewId);
        return ReviewResponse.from(review, reviewAuthor, images, isOwner, likeCount, isLiked);
    }

    @Transactional
    public void deleteReview(Long shopId, Long reviewId, User currentUser) {
        log.info("Deleting review {} for shop {} by user {}", reviewId, shopId, currentUser.getId());

        // 1. Review 조회
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ReviewException.notFound(reviewId));

        // 2. shopId 검증 (보안: 다른 shop의 리뷰 삭제 방지)
        if (!review.getShop().getId().equals(shopId)) {
            log.warn("Review {} does not belong to shop {} (actual: {})",
                    reviewId, shopId, review.getShop().getId());
            throw ReviewException.notFound(reviewId);
        }

        // 3. 권한 확인 (본인 또는 ADMIN)
        if (!review.getUser().getId().equals(currentUser.getId()) && !currentUser.isAdmin()) {
            log.warn("Unauthorized delete attempt for review {} by user {}", reviewId, currentUser.getId());
            throw ReviewException.unauthorized();
        }

        // 4. 이미지 파일 삭제 (클라우드 스토리지 + DB)
        deleteReviewImages(reviewId);

        // 5. ReviewLike 삭제 (FK 제약조건 위반 방지)
        reviewLikeRepository.deleteAllByReviewId(reviewId);
        log.info("Deleted all likes for review {}", reviewId);

        // 6. Review 삭제
        reviewRepository.delete(review);

        log.info("Review {} deleted successfully", reviewId);
    }

    public ReviewImageListResponse getShopReviewImages(Long shopId) {
        log.info("Getting all review images for shop {}", shopId);

        // 1. Shop 존재 확인
        if (!shopRepository.existsById(shopId)) {
            throw ShopException.notFound(shopId);
        }

        // 2. 해당 가게의 모든 리뷰 이미지 조회 (최신순)
        List<ReviewImage> images = reviewImageRepository
                .findAllByShopIdOrderByCreatedAtDesc(shopId);

        log.info("Found {} review images for shop {}", images.size(), shopId);
        return ReviewImageListResponse.from(images);
    }

    private void deleteReviewImages(Long reviewId) {
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderByDisplayOrder(reviewId);

        for (ReviewImage image : images) {
            try {
                fileStorageService.deleteFile(image.getImageUrl());
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
