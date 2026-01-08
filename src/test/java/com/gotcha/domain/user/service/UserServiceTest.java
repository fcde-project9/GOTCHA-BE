package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.comment.repository.CommentRepository;
import com.gotcha.domain.favorite.repository.FavoriteRepository;
import com.gotcha.domain.file.service.FileUploadService;
import com.gotcha.domain.review.repository.ReviewImageRepository;
import com.gotcha.domain.review.repository.ReviewRepository;
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
        @DisplayName("회원 탈퇴 성공 - 설문 저장, 관련 데이터 삭제, soft delete")
        void withdraw_Success() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(testUser);
            when(reviewRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            WithdrawalRequest request = new WithdrawalRequest(WithdrawalReason.LOW_USAGE, "사용 빈도가 낮아서");

            // when
            userService.withdraw(request);

            // then
            verify(withdrawalSurveyRepository).save(any(WithdrawalSurvey.class));
            verify(favoriteRepository).deleteByUserId(testUser.getId());
            verify(reviewRepository).deleteByUserId(testUser.getId());
            verify(commentRepository).deleteByUserId(testUser.getId());
            verify(refreshTokenRepository).deleteByUserId(testUser.getId());
            assertThat(testUser.getIsDeleted()).isTrue();
            assertThat(testUser.getNickname()).startsWith("탈퇴한 사용자_");
        }

        @Test
        @DisplayName("회원 탈퇴 성공 - detail 없이 탈퇴")
        void withdraw_Success_WithoutDetail() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(testUser);
            when(reviewRepository.findAllByUserId(testUser.getId())).thenReturn(Collections.emptyList());
            WithdrawalRequest request = new WithdrawalRequest(WithdrawalReason.NO_DESIRED_INFO, null);

            // when
            userService.withdraw(request);

            // then
            verify(withdrawalSurveyRepository).save(any(WithdrawalSurvey.class));
            verify(refreshTokenRepository).deleteByUserId(testUser.getId());
            assertThat(testUser.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자 - U005 예외 발생")
        void withdraw_AlreadyDeleted_ThrowsException() {
            // given
            testUser.delete(); // 이미 탈퇴된 상태로 설정
            when(securityUtil.getCurrentUser()).thenReturn(testUser);
            WithdrawalRequest request = new WithdrawalRequest(WithdrawalReason.OTHER, "기타 사유");

            // when & then
            assertThatThrownBy(() -> userService.withdraw(request))
                    .isInstanceOf(UserException.class)
                    .hasMessageContaining("이미 탈퇴한 사용자입니다");
        }
    }
}
