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
        long accessTokenValidity = 900000L; // 15분
        long refreshTokenValidity = 604800000L; // 7일

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
    @DisplayName("validateToken 일관성 테스트")
    class ValidateTokenConsistencyTest {

        @Test
        @DisplayName("만료된 토큰은 예외를 던지고, 잘못된 토큰은 false를 반환 - 일관성 없는 동작 문서화")
        void validateToken_inconsistentBehavior_documented() {
            // given
            JwtTokenProvider expiredProvider = new JwtTokenProvider(
                    "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long",
                    -1000L,
                    -1000L
            );
            String expiredToken = expiredProvider.generateAccessToken(testUser);
            String malformedToken = "invalid.token.format";

            // when & then
            // 만료된 토큰 -> 예외 throw
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                    .isInstanceOf(AuthException.class);

            // 잘못된 토큰 -> false 반환 (예외 아님)
            boolean result = jwtTokenProvider.validateToken(malformedToken);
            assertThat(result).isFalse();

            // 이 테스트는 두 경우의 동작이 다름을 문서화
            // 일관성을 위해서는 둘 다 예외를 던지거나 둘 다 false를 반환해야 함
        }

        @Test
        @DisplayName("다양한 유효하지 않은 토큰 유형별 동작 검증")
        void validateToken_variousInvalidTokenTypes() {
            // given
            String nullToken = null;
            String emptyToken = "";
            String blankToken = "   ";
            String malformedToken = "not.a.jwt";
            String wrongSignatureToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.wrong-signature";

            // when & then - 모두 false 반환 (예외 없음)
            assertThat(jwtTokenProvider.validateToken(nullToken)).isFalse();
            assertThat(jwtTokenProvider.validateToken(emptyToken)).isFalse();
            assertThat(jwtTokenProvider.validateToken(blankToken)).isFalse();
            assertThat(jwtTokenProvider.validateToken(malformedToken)).isFalse();
            assertThat(jwtTokenProvider.validateToken(wrongSignatureToken)).isFalse();
        }

        @Test
        @DisplayName("만료된 토큰만 예외를 던짐 - 특수 케이스")
        void validateToken_onlyExpiredTokenThrowsException() {
            // given
            JwtTokenProvider expiredProvider = new JwtTokenProvider(
                    "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long",
                    -1000L,
                    -1000L
            );
            String expiredToken = expiredProvider.generateAccessToken(testUser);

            // when & then
            // 만료된 토큰만 예외를 던지고, 나머지는 false 반환
            // 이는 호출자가 예외 처리와 boolean 체크를 모두 해야 함을 의미
            assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("만료");
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
