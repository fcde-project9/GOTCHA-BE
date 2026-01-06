package com.gotcha.domain.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        String testSecret = "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long";
        long accessTokenValidity = 3600000L; // 1시간
        long refreshTokenValidity = 1209600000L; // 14일

        jwtTokenProvider = new JwtTokenProvider(testSecret, accessTokenValidity, refreshTokenValidity);

        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .build();
        // User의 id는 protected 접근자로 인해 리플렉션을 사용하여 설정
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
    @DisplayName("generateAccessToken")
    class GenerateAccessToken {

        @Test
        @DisplayName("유효한 사용자로 Access Token을 생성한다")
        void shouldGenerateAccessToken() {
            // when
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    class GenerateRefreshToken {

        @Test
        @DisplayName("유효한 사용자로 Refresh Token을 생성한다")
        void shouldGenerateRefreshToken() {
            // when
            String token = jwtTokenProvider.generateRefreshToken(testUser);

            // then
            assertThat(token).isNotNull();
            assertThat(token).isNotBlank();
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateToken {

        @Test
        @DisplayName("유효한 토큰은 true를 반환한다")
        void shouldReturnTrueForValidToken() {
            // given
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // when
            boolean isValid = jwtTokenProvider.validateToken(token);

            // then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("잘못된 형식의 토큰은 false를 반환한다")
        void shouldReturnFalseForMalformedToken() {
            // given
            String invalidToken = "invalid.token.format";

            // when
            boolean isValid = jwtTokenProvider.validateToken(invalidToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("빈 토큰은 false를 반환한다")
        void shouldReturnFalseForEmptyToken() {
            // when
            boolean isValid = jwtTokenProvider.validateToken("");

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("null 토큰은 false를 반환한다")
        void shouldReturnFalseForNullToken() {
            // when
            boolean isValid = jwtTokenProvider.validateToken(null);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰은 AuthException을 던진다")
        void shouldThrowExceptionForExpiredToken() {
            // given - 만료 시간을 음수로 설정한 provider
            JwtTokenProvider expiredProvider = new JwtTokenProvider(
                    "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long",
                    -1000L, // 이미 만료됨
                    -1000L
            );
            String expiredToken = expiredProvider.generateAccessToken(testUser);

            // when & then
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                    .isInstanceOf(AuthException.class);
        }
    }

    @Nested
    @DisplayName("getUserIdFromToken")
    class GetUserIdFromToken {

        @Test
        @DisplayName("유효한 토큰에서 사용자 ID를 추출한다")
        void shouldExtractUserIdFromValidToken() {
            // given
            String token = jwtTokenProvider.generateAccessToken(testUser);

            // when
            Long userId = jwtTokenProvider.getUserIdFromToken(token);

            // then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Access Token과 Refresh Token 모두에서 동일한 사용자 ID를 추출한다")
        void shouldExtractSameUserIdFromBothTokens() {
            // given
            String accessToken = jwtTokenProvider.generateAccessToken(testUser);
            String refreshToken = jwtTokenProvider.generateRefreshToken(testUser);

            // when
            Long accessUserId = jwtTokenProvider.getUserIdFromToken(accessToken);
            Long refreshUserId = jwtTokenProvider.getUserIdFromToken(refreshToken);

            // then
            assertThat(accessUserId).isEqualTo(refreshUserId);
            assertThat(accessUserId).isEqualTo(1L);
        }
    }
}
