package com.gotcha.domain.user.service;

import com.gotcha._global.common.PageResponse;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.auth.service.SocialUnlinkService;
import com.gotcha.domain.comment.repository.CommentRepository;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.file.service.FileStorageService;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewLikeRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.shop.repository.ShopRepository;
import com.gotcha.domain.shop.service.ShopService;
import com.gotcha.domain.user.dto.MyShopResponse;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.WithdrawalSurvey;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserPermissionRepository;
import com.gotcha.domain.user.repository.UserRepository;
import com.gotcha.domain.user.repository.WithdrawalSurveyRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final WithdrawalSurveyRepository withdrawalSurveyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final CommentRepository commentRepository;
    private final FileStorageService fileStorageService;
    private final ShopRepository shopRepository;
    private final ShopService shopService;
    private final SocialUnlinkService socialUnlinkService;

    @Value("${user.default-profile-image-url}")
    private String defaultProfileImageUrl;

    public UserResponse getMyInfo() {
        User user = securityUtil.getCurrentUser();
        return UserResponse.from(user, defaultProfileImageUrl);
    }

    /**
     * 현재 로그인한 사용자의 닉네임 조회
     * @return 사용자 닉네임
     */
    public UserNicknameResponse getNickname() {
        User user = securityUtil.getCurrentUser();
        return UserNicknameResponse.from(user);
    }

    /**
     * 내가 제보한 가게 목록 조회
     * @param pageable 페이징 정보
     * @return 내가 제보한 가게 목록
     */
    public PageResponse<MyShopResponse> getMyShops(Pageable pageable) {
        Long userId = securityUtil.getCurrentUserId();

        Page<Shop> shopPage = shopRepository.findAllByCreatedByIdWithUser(userId, pageable);

        List<MyShopResponse> content = shopPage.getContent().stream()
                .map(shop -> {

                    Boolean isOpen = shopService.isOpenNow(shop.getOpenTime());

                    return MyShopResponse.from(shop, isOpen);
                })
                .collect(Collectors.toList());

        return PageResponse.from(shopPage, content);
    }

    /**
     * 닉네임 변경
     * @param nickname 새 닉네임
     * @return 변경된 사용자 정보
     * @throws UserException 닉네임이 중복된 경우 (U001)
     */
    @Transactional
    public UserResponse updateNickname(String nickname) {
        log.info("updateNickname - nickname: {}", nickname);

        // 현재 로그인한 사용자 조회
        User currentUser = securityUtil.getCurrentUser();
        log.info("Current user ID: {}, current nickname: {}", currentUser.getId(), currentUser.getNickname());

        // 현재 닉네임과 동일한 경우 그대로 반환 (중복 체크 불필요)
        if (currentUser.getNickname().equals(nickname)) {
            log.info("Same nickname as current, skipping duplicate check");
            return UserResponse.from(currentUser, defaultProfileImageUrl);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            log.warn("Duplicate nickname: {}", nickname);
            throw UserException.duplicateNickname(nickname);
        }

        // 닉네임 변경
        currentUser.updateNickname(nickname);
        log.info("Nickname updated successfully: {} -> {}", currentUser.getId(), nickname);

        return UserResponse.from(currentUser, defaultProfileImageUrl);
    }

    /**
     * 프로필 이미지 변경
     * @param profileImageUrl 새 프로필 이미지 URL
     * @return 변경된 사용자 정보
     */
    @Transactional
    public UserResponse updateProfileImage(String profileImageUrl) {
        log.info("updateProfileImage - profileImageUrl: {}", profileImageUrl);

        // 현재 로그인한 사용자 조회
        User currentUser = securityUtil.getCurrentUser();
        String oldImageUrl = currentUser.getProfileImageUrl();
        log.info("Current user ID: {}, old profileImageUrl: {}", currentUser.getId(), oldImageUrl);

        // 기존 이미지가 있고 기본 이미지가 아닌 경우 GCS에서 삭제
        if (oldImageUrl != null && !oldImageUrl.contains("/defaults/")) {
            try {
                fileStorageService.deleteFile(oldImageUrl);
                log.info("Deleted old profile image: {}", oldImageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old profile image: {} - {}", oldImageUrl, e.getMessage());
                // 삭제 실패해도 계속 진행 (이미 삭제된 파일일 수 있음)
            }
        }

        // 프로필 이미지 변경
        currentUser.updateProfileImage(profileImageUrl);
        log.info("Profile image updated successfully: {} -> {}", currentUser.getId(), profileImageUrl);

        return UserResponse.from(currentUser, defaultProfileImageUrl);
    }

    /**
     * 프로필 이미지 삭제 (기본 이미지로 복구)
     * @return 변경된 사용자 정보
     */
    @Transactional
    public UserResponse deleteProfileImage() {
        log.info("deleteProfileImage - resetting to default");

        // 현재 로그인한 사용자 조회
        User currentUser = securityUtil.getCurrentUser();
        String oldImageUrl = currentUser.getProfileImageUrl();
        log.info("Current user ID: {}, old profileImageUrl: {}", currentUser.getId(), oldImageUrl);

        // 기존 커스텀 이미지 GCS에서 삭제 (기본 이미지는 제외)
        if (oldImageUrl != null && !oldImageUrl.contains("/defaults/")) {
            try {
                fileStorageService.deleteFile(oldImageUrl);
                log.info("Deleted custom profile image: {}", oldImageUrl);
            } catch (Exception e) {
                log.warn("Failed to delete old profile image: {} - {}", oldImageUrl, e.getMessage());
                // 삭제 실패해도 계속 진행 (이미 삭제된 파일일 수 있음)
            }
        }

        // 기본 프로필 이미지로 복구
        currentUser.updateProfileImage(defaultProfileImageUrl);
        log.info("Profile image reset to default: {} -> {}", currentUser.getId(), defaultProfileImageUrl);

        return UserResponse.from(currentUser, defaultProfileImageUrl);
    }

    /**
     * 회원 탈퇴
     * 1. 소셜 계정 연결 끊기 (카카오)
     * 2. 탈퇴 설문 저장
     * 3. 찜 목록 삭제
     * 4. 사용자가 누른 리뷰 좋아요 삭제
     * 5. 리뷰 이미지 GCS 삭제 및 DB 삭제 + 사용자 리뷰에 달린 좋아요 삭제
     * 6. 리뷰 삭제
     * 7. 댓글 삭제
     * 8. 권한 동의 기록 삭제
     * 9. RefreshToken 삭제
     * 10. 사용자 soft delete (개인정보 마스킹)
     *
     * @param request 탈퇴 설문 정보 (reason 필수, detail 선택)
     * @throws UserException 이미 탈퇴한 사용자인 경우 (U005)
     */
    @Transactional
    public void withdraw(WithdrawalRequest request) {
        // SecurityUtil.getCurrentUser() 대신 직접 조회 (탈퇴 후 응답 시 A012 에러 방지)
        Long userId = securityUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));
        log.info("withdraw - userId: {}, reasons: {}", userId, request.reasons());

        // 이미 탈퇴한 사용자인지 확인
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            log.warn("Already deleted user attempted withdrawal - userId: {}", userId);
            throw UserException.alreadyDeleted(userId);
        }

        // 1. 소셜 계정 연결 끊기 (user.delete() 전에 호출해야 socialType/socialId 접근 가능)
        socialUnlinkService.unlinkSocialAccount(user);
        log.info("Social account unlinked - userId: {}", userId);

        // 2. 탈퇴 설문 저장
        WithdrawalSurvey survey = WithdrawalSurvey.builder()
                .user(user)
                .reasons(request.reasons())
                .detail(request.detail())
                .build();
        withdrawalSurveyRepository.save(survey);
        log.info("Withdrawal survey saved - surveyId: {}, userId: {}", survey.getId(), userId);

        // 3. 찜 목록 삭제
        favoriteRepository.deleteByUserId(userId);
        log.info("Favorites deleted - userId: {}", userId);

        // 4. 사용자가 누른 리뷰 좋아요 삭제 (다른 사람 리뷰에 누른 좋아요)
        reviewLikeRepository.deleteByUserId(userId);
        log.info("User's review likes deleted - userId: {}", userId);

        // 5. 리뷰 이미지 삭제 (GCS + DB) + 사용자 리뷰에 달린 좋아요 삭제
        deleteUserReviewImages(userId);

        // 6. 리뷰 삭제
        reviewRepository.deleteByUserId(userId);
        log.info("Reviews deleted - userId: {}", userId);

        // 7. 댓글 삭제
        commentRepository.deleteByUserId(userId);
        log.info("Comments deleted - userId: {}", userId);

        // 8. 권한 동의 기록 삭제
        userPermissionRepository.deleteByUserId(userId);
        log.info("User permissions deleted - userId: {}", userId);

        // 9. RefreshToken 삭제
        refreshTokenRepository.deleteByUserId(userId);
        log.info("RefreshToken deleted - userId: {}", userId);

        // 10. 사용자 soft delete (개인정보 마스킹 포함)
        user.delete();
        userRepository.save(user);
        log.info("User soft deleted with masked info - userId: {}", userId);
    }

    /**
     * 사용자의 모든 리뷰 관련 데이터 삭제 (이미지 + 좋아요)
     * - 리뷰 이미지: GCS 파일 삭제 + DB 삭제
     * - 리뷰 좋아요: 해당 리뷰들에 달린 모든 좋아요 삭제 (다른 사용자의 좋아요 포함)
     */
    private void deleteUserReviewImages(Long userId) {
        // 사용자의 모든 리뷰 조회
        List<Review> userReviews = reviewRepository.findAllByUserId(userId);
        if (userReviews.isEmpty()) {
            log.info("No reviews found for user - userId: {}", userId);
            return;
        }

        List<Long> reviewIds = userReviews.stream().map(Review::getId).toList();

        // 리뷰 이미지 URL 조회
        List<ReviewImage> reviewImages = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds);

        // GCS에서 이미지 파일 삭제
        for (ReviewImage image : reviewImages) {
            try {
                fileStorageService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image from GCS: {} - {}", image.getImageUrl(), e.getMessage());
            }
        }
        log.info("GCS images deleted - userId: {}, count: {}", userId, reviewImages.size());

        // DB에서 리뷰 이미지 삭제
        reviewImageRepository.deleteAllByReviewIdIn(reviewIds);
        log.info("Review images deleted from DB - userId: {}, reviewCount: {}", userId, reviewIds.size());

        // 리뷰 좋아요 삭제 (다른 사용자가 누른 좋아요 포함)
        reviewLikeRepository.deleteAllByReviewIdIn(reviewIds);
        log.info("Review likes deleted - userId: {}, reviewCount: {}", userId, reviewIds.size());
    }
}
