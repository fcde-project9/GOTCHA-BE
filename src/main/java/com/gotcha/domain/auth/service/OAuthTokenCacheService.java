package com.gotcha.domain.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.UUID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * OAuth 로그인 완료 후 임시 코드와 토큰을 매핑하는 캐시 서비스.
 *
 * 보안 강화:
 * - URL에 토큰 노출 대신 1회용 임시 코드 사용
 * - 30초 TTL로 자동 만료
 * - 코드 사용 즉시 캐시에서 삭제 (1회용)
 */
@Slf4j
@Service
public class OAuthTokenCacheService {

    private static final int CODE_EXPIRE_SECONDS = 30;
    private static final int MAX_CACHE_SIZE = 10000;

    private final Cache<String, TokenData> tokenCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(CODE_EXPIRE_SECONDS))
            .maximumSize(MAX_CACHE_SIZE)
            .build();

    /**
     * 토큰을 캐시에 저장하고 임시 코드 반환
     *
     * @param accessToken  액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param isNewUser    신규 사용자 여부
     * @return 임시 코드 (UUID)
     */
    public String storeTokens(String accessToken, String refreshToken, boolean isNewUser) {
        String code = UUID.randomUUID().toString();
        TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);
        tokenCache.put(code, tokenData);
        log.debug("Stored tokens with temp code: {}", code);
        return code;
    }

    /**
     * 임시 코드로 토큰 조회 및 삭제 (1회용)
     *
     * @param code 임시 코드
     * @return 토큰 데이터 (없거나 만료된 경우 null)
     */
    public TokenData exchangeCode(String code) {
        TokenData tokenData = tokenCache.getIfPresent(code);
        if (tokenData != null) {
            tokenCache.invalidate(code);
            log.debug("Exchanged and invalidated temp code: {}", code);
        } else {
            log.warn("Invalid or expired temp code: {}", code);
        }
        return tokenData;
    }

    /**
     * 토큰 데이터를 담는 불변 객체
     */
    @Getter
    public static class TokenData {
        private final String accessToken;
        private final String refreshToken;
        private final boolean isNewUser;

        public TokenData(String accessToken, String refreshToken, boolean isNewUser) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.isNewUser = isNewUser;
        }
    }
}
