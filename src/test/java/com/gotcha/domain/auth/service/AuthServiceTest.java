package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.dto.TokenExchangeResponse;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.repository.RedisRefreshTokenStore;
import com.gotcha.domain.auth.service.OAuthTokenCookieService.TokenData;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisRefreshTokenStore redisRefreshTokenStore;

    @Mock
    private OAuthTokenCookieService oAuthTokenCacheService;

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("reissueToken")
    class ReissueToken {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 토큰을 발급한다")
        void shouldReissueTokenWithValidRefreshToken() {
            // given
            String refreshTokenValue = "valid-refresh-token";
            String newAccessToken = "new-access-token";
            String newRefreshToken = "new-refresh-token";

            given(redisRefreshTokenStore.findUserIdByToken(refreshTokenValue))
                    .willReturn(Optional.of(1L));
            given(userRepository.findById(1L))
                    .willReturn(Optional.of(testUser));
            given(jwtTokenProvider.generateAccessToken(testUser))
                    .willReturn(newAccessToken);
            given(jwtTokenProvider.generateRefreshToken(testUser))
                    .willReturn(newRefreshToken);

            // when
            TokenResponse response = authService.reissueToken(refreshTokenValue);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(newAccessToken);
            assertThat(response.refreshToken()).isEqualTo(newRefreshToken);
            assertThat(response.user().id()).isEqualTo(1L);
            assertThat(response.user().isNewUser()).isFalse();
            verify(redisRefreshTokenStore).save(1L, newRefreshToken);
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰이면 예외를 던진다")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            // given
            String invalidToken = "non-existent-token";
            given(redisRefreshTokenStore.findUserIdByToken(invalidToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(invalidToken))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("리프레시 토큰");
        }

        @Test
        @DisplayName("만료된 리프레시 토큰은 Redis TTL로 키가 소멸되어 not found와 동일하게 처리된다")
        void shouldThrowExceptionWhenRefreshTokenExpired() {
            // given - Redis TTL 만료 시 키가 없으므로 empty 반환
            String expiredToken = "expired-refresh-token";
            given(redisRefreshTokenStore.findUserIdByToken(expiredToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(expiredToken))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("리프레시 토큰");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("사용자 ID로 리프레시 토큰을 삭제한다")
        void shouldDeleteRefreshTokenByUserId() {
            // given
            Long userId = 1L;

            // when
            authService.logout(userId);

            // then
            verify(redisRefreshTokenStore).deleteByUserId(userId);
        }
    }

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshToken {

        @Test
        @DisplayName("리프레시 토큰을 Redis에 저장한다")
        void shouldSaveRefreshToken() {
            // given
            String token = "new-refresh-token";

            // when
            authService.saveRefreshToken(testUser, token);

            // then
            verify(redisRefreshTokenStore).save(testUser.getId(), token);
        }
    }

    @Nested
    @DisplayName("exchangeToken")
    class ExchangeToken {

        @Test
        @DisplayName("유효한 암호화 코드로 토큰을 교환한다")
        void shouldExchangeTokenWithValidCode() {
            // given
            String encryptedCode = "encrypted-code";
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            boolean isNewUser = true;

            TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);
            given(oAuthTokenCacheService.decryptTokens(encryptedCode))
                    .willReturn(tokenData);

            // when
            TokenExchangeResponse tokenResponse = authService.exchangeToken(encryptedCode);

            // then
            assertThat(tokenResponse).isNotNull();
            assertThat(tokenResponse.accessToken()).isEqualTo(accessToken);
            assertThat(tokenResponse.refreshToken()).isEqualTo(refreshToken);
            assertThat(tokenResponse.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("기존 사용자인 경우 isNewUser가 false")
        void shouldReturnIsNewUserFalseForExistingUser() {
            // given
            String encryptedCode = "encrypted-code";
            TokenData tokenData = new TokenData("access", "refresh", false);
            given(oAuthTokenCacheService.decryptTokens(encryptedCode))
                    .willReturn(tokenData);

            // when
            TokenExchangeResponse tokenResponse = authService.exchangeToken(encryptedCode);

            // then
            assertThat(tokenResponse.isNewUser()).isFalse();
        }

        @Test
        @DisplayName("유효하지 않은 코드이면 예외를 던진다")
        void shouldThrowExceptionWithInvalidCode() {
            // given
            String invalidCode = "invalid-code";
            given(oAuthTokenCacheService.decryptTokens(invalidCode))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.exchangeToken(invalidCode))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("인증 코드");
        }

        @Test
        @DisplayName("복호화 실패 시 예외를 던진다")
        void shouldThrowExceptionWhenDecryptionFails() {
            // given
            String corruptedCode = "corrupted-code";
            given(oAuthTokenCacheService.decryptTokens(corruptedCode))
                    .willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.exchangeToken(corruptedCode))
                    .isInstanceOf(AuthException.class);
        }
    }
}
