package com.gotcha.domain.user.service;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.comment.repository.CommentRepository;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.file.service.FileUploadService;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.user.dto.UserNicknameResponse;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.WithdrawalSurvey;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import com.gotcha.domain.user.repository.WithdrawalSurveyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;
    private final WithdrawalSurveyRepository withdrawalSurveyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CommentRepository commentRepository;
    private final FileUploadService fileUploadService;

    public UserResponse getMyInfo() {
        User user = securityUtil.getCurrentUser();
        return UserResponse.from(user);
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
            return UserResponse.from(currentUser);
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(nickname)) {
            log.warn("Duplicate nickname: {}", nickname);
            throw UserException.duplicateNickname(nickname);
        }

        // 닉네임 변경
        currentUser.updateNickname(nickname);
        log.info("Nickname updated successfully: {} -> {}", currentUser.getId(), nickname);

        return UserResponse.from(currentUser);
    }

    /**
     * 회원 탈퇴
     * 1. 탈퇴 설문 저장
     * 2. 찜 목록 삭제
     * 3. 리뷰 이미지 GCS 삭제 및 DB 삭제
     * 4. 리뷰 삭제
     * 5. 댓글 삭제
     * 6. RefreshToken 삭제
     * 7. 사용자 soft delete (개인정보 마스킹)
     *
     * @param request 탈퇴 설문 정보 (reason 필수, detail 선택)
     * @throws UserException 이미 탈퇴한 사용자인 경우 (U005)
     */
    @Transactional
    public void withdraw(WithdrawalRequest request) {
        User user = securityUtil.getCurrentUser();
        Long userId = user.getId();
        log.info("withdraw - userId: {}, reason: {}", userId, request.reason());

        // 이미 탈퇴한 사용자인지 확인
        if (Boolean.TRUE.equals(user.getIsDeleted())) {
            log.warn("Already deleted user attempted withdrawal - userId: {}", userId);
            throw UserException.alreadyDeleted(userId);
        }

        // 1. 탈퇴 설문 저장
        WithdrawalSurvey survey = WithdrawalSurvey.builder()
                .user(user)
                .reason(request.reason())
                .detail(request.detail())
                .build();
        withdrawalSurveyRepository.save(survey);
        log.info("Withdrawal survey saved - surveyId: {}, userId: {}", survey.getId(), userId);

        // 2. 찜 목록 삭제
        favoriteRepository.deleteByUserId(userId);
        log.info("Favorites deleted - userId: {}", userId);

        // 3. 리뷰 이미지 삭제 (GCS + DB)
        deleteUserReviewImages(userId);

        // 4. 리뷰 삭제
        reviewRepository.deleteByUserId(userId);
        log.info("Reviews deleted - userId: {}", userId);

        // 5. 댓글 삭제
        commentRepository.deleteByUserId(userId);
        log.info("Comments deleted - userId: {}", userId);

        // 6. RefreshToken 삭제
        refreshTokenRepository.deleteByUserId(userId);
        log.info("RefreshToken deleted - userId: {}", userId);

        // 7. 사용자 soft delete (개인정보 마스킹 포함)
        user.delete();
        log.info("User soft deleted with masked info - userId: {}", userId);
    }

    /**
     * 사용자의 모든 리뷰 이미지 삭제 (GCS + DB)
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
                fileUploadService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image from GCS: {} - {}", image.getImageUrl(), e.getMessage());
            }
        }
        log.info("GCS images deleted - userId: {}, count: {}", userId, reviewImages.size());

        // DB에서 리뷰 이미지 삭제
        reviewImageRepository.deleteAllByReviewIdIn(reviewIds);
        log.info("Review images deleted from DB - userId: {}, reviewCount: {}", userId, reviewIds.size());
    }
}
