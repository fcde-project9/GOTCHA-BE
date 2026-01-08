package com.gotcha.domain.auth.service;

import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.entity.RefreshToken;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.repository.RefreshTokenRepository;
import com.gotcha.domain.user.entity.User;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Transactional
    public TokenResponse reissueToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(AuthException::refreshTokenNotFound);

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw AuthException.refreshTokenExpired();
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValidity / 1000);
        refreshToken.updateToken(newRefreshToken, expiresAt);

        return TokenResponse.of(newAccessToken, newRefreshToken, user, false);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    @Transactional
    public void saveRefreshToken(User user, String token) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenValidity / 1000);

        refreshTokenRepository.findByUser(user)
                .ifPresentOrElse(
                        existingToken -> existingToken.updateToken(token, expiresAt),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .user(user)
                                        .token(token)
                                        .expiresAt(expiresAt)
                                        .build()
                        )
                );
    }
}
