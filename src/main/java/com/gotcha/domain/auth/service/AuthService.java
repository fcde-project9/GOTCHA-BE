package com.gotcha.domain.auth.service;

import com.gotcha.domain.auth.dto.TokenExchangeResponse;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.repository.RedisRefreshTokenStore;
import com.gotcha.domain.auth.service.OAuthTokenCookieService.TokenData;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.exception.UserException;
import com.gotcha.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisRefreshTokenStore redisRefreshTokenStore;
    private final OAuthTokenCookieService oAuthTokenCacheService;
    private final UserRepository userRepository;

    /**
     * 토큰 재발급 (기존 refresh token 검증 후 access/refresh token 모두 재발급)
     */
    public TokenResponse reissueToken(String refreshTokenValue) {
        Long userId = redisRefreshTokenStore.findUserIdByToken(refreshTokenValue)
                .orElseThrow(AuthException::refreshTokenNotFound);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> UserException.notFound(userId));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        redisRefreshTokenStore.save(userId, newRefreshToken);

        return TokenResponse.of(newAccessToken, newRefreshToken, user, false);
    }

    /**
     * 로그아웃 (Redis에서 refresh token 삭제)
     */
    public void logout(Long userId) {
        redisRefreshTokenStore.deleteByUserId(userId);
    }

    /**
     * Refresh token 저장 - 로그인/토큰 발급 시 refresh token을 redis에 저장
     */
    public void saveRefreshToken(User user, String token) {
        redisRefreshTokenStore.save(user.getId(), token);
    }

    /**
     * OAuth 암호화된 코드를 토큰으로 교환
     *
     * @param encryptedCode 암호화된 토큰 코드 (URL 파라미터로 전달받은 값)
     * @return 토큰 응답
     * @throws AuthException 유효하지 않거나 복호화 실패한 코드인 경우
     */
    public TokenExchangeResponse exchangeToken(String encryptedCode) {
        TokenData tokenData = oAuthTokenCacheService.decryptTokens(encryptedCode);
        if (tokenData == null) {
            throw AuthException.invalidAuthCode();
        }
        return TokenExchangeResponse.of(
                tokenData.getAccessToken(),
                tokenData.getRefreshToken(),
                tokenData.isNewUser()
        );
    }
}
