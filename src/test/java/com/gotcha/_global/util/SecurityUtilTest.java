package com.gotcha._global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityUtilTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityUtil securityUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .build();
        setUserId(testUser, 1L);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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

    private void setAuthentication(Long userId) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Nested
    @DisplayName("getCurrentUser")
    class GetCurrentUser {

        @Test
        @DisplayName("인증된 사용자의 User 엔티티를 반환한다")
        void shouldReturnUserWhenAuthenticated() {
            // given
            setAuthentication(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when
            User result = securityUtil.getCurrentUser();

            // then
            assertThat(result).isEqualTo(testUser);
            assertThat(result.getNickname()).isEqualTo("테스트유저");
        }

        @Test
        @DisplayName("인증되지 않은 경우 AuthException을 던진다")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // given - SecurityContext가 비어있음

            // when & then
            assertThatThrownBy(() -> securityUtil.getCurrentUser())
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 AuthException을 던진다")
        void shouldThrowExceptionWhenUserNotFound() {
            // given
            setAuthentication(999L);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> securityUtil.getCurrentUser())
                    .isInstanceOf(AuthException.class);
        }

        @Test
        @DisplayName("탈퇴한 사용자가 API 접근 시 A012 예외를 던진다")
        void shouldThrowExceptionWhenUserIsDeleted() {
            // given
            testUser.delete(); // 탈퇴 처리
            setAuthentication(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> securityUtil.getCurrentUser())
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("탈퇴한 사용자입니다");
        }
    }

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserId {

        @Test
        @DisplayName("인증된 사용자의 ID를 반환한다")
        void shouldReturnUserIdWhenAuthenticated() {
            // given
            setAuthentication(1L);

            // when
            Long result = securityUtil.getCurrentUserId();

            // then
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("인증되지 않은 경우 AuthException을 던진다")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // given - SecurityContext가 비어있음

            // when & then
            assertThatThrownBy(() -> securityUtil.getCurrentUserId())
                    .isInstanceOf(AuthException.class);
        }
    }
}
