package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.gotcha.domain.auth.dto.TokenExchangeResponse;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.entity.RefreshToken;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.auth.service.OAuthTokenCacheService.TokenData;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.time.LocalDateTime;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private OAuthTokenCacheService oAuthTokenCacheService;

    private User testUser;
    private RefreshToken validRefreshToken;
    private RefreshToken expiredRefreshToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenValidity", 604800000L); // 7일

        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        validRefreshToken = RefreshToken.builder()
                .user(testUser)
                .token("valid-refresh-token")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        expiredRefreshToken = RefreshToken.builder()
                .user(testUser)
                .token("expired-refresh-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
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

            given(refreshTokenRepository.findByToken(refreshTokenValue))
                    .willReturn(Optional.of(validRefreshToken));
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
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰이면 예외를 던진다")
        void shouldThrowExceptionWhenRefreshTokenNotFound() {
            // given
            String invalidToken = "non-existent-token";
            given(refreshTokenRepository.findByToken(invalidToken))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(invalidToken))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("리프레시 토큰");
        }

        @Test
        @DisplayName("만료된 리프레시 토큰이면 삭제 후 예외를 던진다")
        void shouldDeleteAndThrowExceptionWhenRefreshTokenExpired() {
            // given
            String expiredToken = "expired-refresh-token";
            given(refreshTokenRepository.findByToken(expiredToken))
                    .willReturn(Optional.of(expiredRefreshToken));

            // when & then
            assertThatThrownBy(() -> authService.reissueToken(expiredToken))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("만료");

            verify(refreshTokenRepository).delete(expiredRefreshToken);
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
            verify(refreshTokenRepository).deleteByUserId(userId);
        }
    }

    @Nested
    @DisplayName("saveRefreshToken")
    class SaveRefreshToken {

        @Test
        @DisplayName("기존 토큰이 있으면 업데이트한다")
        void shouldUpdateExistingToken() {
            // given
            String newToken = "new-refresh-token";
            given(refreshTokenRepository.findByUser(testUser))
                    .willReturn(Optional.of(validRefreshToken));

            // when
            authService.saveRefreshToken(testUser, newToken);

            // then
            assertThat(validRefreshToken.getToken()).isEqualTo(newToken);
        }

        @Test
        @DisplayName("기존 토큰이 없으면 새로 저장한다")
        void shouldSaveNewTokenWhenNotExists() {
            // given
            String newToken = "new-refresh-token";
            given(refreshTokenRepository.findByUser(testUser))
                    .willReturn(Optional.empty());

            // when
            authService.saveRefreshToken(testUser, newToken);

            // then
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("exchangeToken")
    class ExchangeToken {

        @Test
        @DisplayName("유효한 임시 코드로 토큰을 교환한다")
        void shouldExchangeTokenWithValidCode() {
            // given
            String code = "valid-temp-code";
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            boolean isNewUser = true;

            TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);
            given(oAuthTokenCacheService.exchangeCode(code)).willReturn(tokenData);

            // when
            TokenExchangeResponse response = authService.exchangeToken(code);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
            assertThat(response.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("기존 사용자인 경우 isNewUser가 false")
        void shouldReturnIsNewUserFalseForExistingUser() {
            // given
            String code = "valid-temp-code";
            TokenData tokenData = new TokenData("access", "refresh", false);
            given(oAuthTokenCacheService.exchangeCode(code)).willReturn(tokenData);

            // when
            TokenExchangeResponse response = authService.exchangeToken(code);

            // then
            assertThat(response.isNewUser()).isFalse();
        }

        @Test
        @DisplayName("유효하지 않은 코드이면 예외를 던진다")
        void shouldThrowExceptionWithInvalidCode() {
            // given
            String invalidCode = "invalid-code";
            given(oAuthTokenCacheService.exchangeCode(invalidCode)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.exchangeToken(invalidCode))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("인증 코드");
        }

        @Test
        @DisplayName("만료된 코드이면 예외를 던진다")
        void shouldThrowExceptionWithExpiredCode() {
            // given
            String expiredCode = "expired-code";
            given(oAuthTokenCacheService.exchangeCode(expiredCode)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.exchangeToken(expiredCode))
                    .isInstanceOf(AuthException.class);
        }
    }
}
