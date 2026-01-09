package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.comment.repository.CommentRepository;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.file.service.FileUploadService;
import com.gotcha.domain.review.entity.Review;
import com.gotcha.domain.review.entity.ReviewImage;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
import com.gotcha.domain.shop.entity.Shop;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.dto.WithdrawalRequest;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.WithdrawalReason;
import com.gotcha.domain.user.entity.WithdrawalSurvey;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import com.gotcha.domain.user.repository.WithdrawalSurveyRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WithdrawalSurveyRepository withdrawalSurveyRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        setUserId(testUser, 1L);
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("현재 로그인한 사용자의 정보를 반환한다")
        void shouldReturnCurrentUserInfo() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(testUser);

            // when
            UserResponse result = userService.getMyInfo();

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nickname()).isEqualTo("테스트유저");
            assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
            assertThat(result.socialType()).isEqualTo(SocialType.KAKAO);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("회원 탈퇴 성공 - 설문 저장, 관련 데이터 삭제, soft delete, 마스킹")
        void withdraw_Success() {
            // given
            testUser = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("12345")
                    .nickname("테스트유저")
                    .email("test@example.com")
                    .profileImageUrl("https://example.com/profile.jpg")
                    .build();
            setUserId(testUser, 1L);

            when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
            when(reviewRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            WithdrawalRequest request = new WithdrawalRequest(
                    List.of(WithdrawalReason.LOW_USAGE, WithdrawalReason.INSUFFICIENT_INFO),
                    "사용 빈도가 낮아서");

            // when
            userService.withdraw(request);

            // then - 설문 저장 검증
            ArgumentCaptor<WithdrawalSurvey> surveyCaptor = ArgumentCaptor.forClass(WithdrawalSurvey.class);
            verify(withdrawalSurveyRepository).save(surveyCaptor.capture());
            WithdrawalSurvey savedSurvey = surveyCaptor.getValue();
            assertThat(savedSurvey.getReasons()).containsExactly(
                    WithdrawalReason.LOW_USAGE, WithdrawalReason.INSUFFICIENT_INFO);
            assertThat(savedSurvey.getDetail()).isEqualTo("사용 빈도가 낮아서");
            assertThat(savedSurvey.getUser()).isEqualTo(testUser);

            // then - 데이터 삭제 검증
            verify(favoriteRepository).deleteByUserId(testUser.getId());
            verify(reviewRepository).deleteByUserId(testUser.getId());
            verify(commentRepository).deleteByUserId(testUser.getId());
            verify(refreshTokenRepository).deleteByUserId(testUser.getId());

            // then - soft delete 및 마스킹 검증
            assertThat(testUser.getIsDeleted()).isTrue();
            assertThat(testUser.getNickname()).isEqualTo("탈퇴한 사용자_1");
            assertThat(testUser.getEmail()).isNull();
            assertThat(testUser.getProfileImageUrl()).isNull();
            assertThat(testUser.getSocialId()).isEqualTo("12345"); // socialId는 유지
        }

        @Test
        @DisplayName("회원 탈퇴 성공 - detail 없이 탈퇴 (reasons만 필수)")
        void withdraw_Success_WithoutDetail() {
            // given
            when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
            when(reviewRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            WithdrawalRequest request = new WithdrawalRequest(List.of(WithdrawalReason.INSUFFICIENT_INFO), null);

            // when
            userService.withdraw(request);

            // then - 설문 저장 검증 (detail은 null)
            ArgumentCaptor<WithdrawalSurvey> surveyCaptor = ArgumentCaptor.forClass(WithdrawalSurvey.class);
            verify(withdrawalSurveyRepository).save(surveyCaptor.capture());
            WithdrawalSurvey savedSurvey = surveyCaptor.getValue();
            assertThat(savedSurvey.getReasons()).containsExactly(WithdrawalReason.INSUFFICIENT_INFO);
            assertThat(savedSurvey.getDetail()).isNull();

            // then - 데이터 삭제 및 soft delete
            verify(refreshTokenRepository).deleteByUserId(testUser.getId());
            assertThat(testUser.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자 - U005 예외 발생")
        void withdraw_AlreadyDeleted_ThrowsException() {
            // given
            testUser.delete(); // 이미 탈퇴된 상태로 설정
            when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
            WithdrawalRequest request = new WithdrawalRequest(List.of(WithdrawalReason.OTHER), "기타 사유");

            // when & then
            assertThatThrownBy(() -> userService.withdraw(request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("이미 탈퇴한 사용자입니다");
        }

        @Test
        @DisplayName("회원 탈퇴 시 리뷰 이미지 GCS 삭제 및 DB 삭제")
        void withdraw_WithReviewImages_DeletesGCSAndDB() {
            // given
            when(securityUtil.getCurrentUserId()).thenReturn(testUser.getId());
            when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));

            // 리뷰 mock 설정
            Review mockReview = Review.builder()
                    .shop(Shop.builder().name("테스트샵").addressName("주소").latitude(37.0).longitude(127.0).createdBy(testUser).build())
                    .user(testUser)
                    .content("리뷰 내용")
                    .build();
            setReviewId(mockReview, 100L);

            ReviewImage mockImage1 = ReviewImage.builder()
                    .review(mockReview)
                    .imageUrl("https://storage.googleapis.com/bucket/image1.jpg")
                    .displayOrder(0)
                    .build();
            ReviewImage mockImage2 = ReviewImage.builder()
                    .review(mockReview)
                    .imageUrl("https://storage.googleapis.com/bucket/image2.jpg")
                    .displayOrder(1)
                    .build();

            when(reviewRepository.findAllByUserId(testUser.getId())).thenReturn(List.of(mockReview));
            when(reviewImageRepository.findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(List.of(100L)))
                    .thenReturn(List.of(mockImage1, mockImage2));

            WithdrawalRequest request = new WithdrawalRequest(List.of(WithdrawalReason.LOW_USAGE), null);

            // when
            userService.withdraw(request);

            // then - GCS 이미지 삭제 검증
            verify(fileUploadService).deleteFile("https://storage.googleapis.com/bucket/image1.jpg");
            verify(fileUploadService).deleteFile("https://storage.googleapis.com/bucket/image2.jpg");

            // then - DB 이미지 삭제 검증
            verify(reviewImageRepository).deleteAllByReviewIdIn(List.of(100L));

            // then - 리뷰 삭제 검증
            verify(reviewRepository).deleteByUserId(testUser.getId());
        }

        private void setReviewId(Review review, Long id) {
            try {
                var field = Review.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(review, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
