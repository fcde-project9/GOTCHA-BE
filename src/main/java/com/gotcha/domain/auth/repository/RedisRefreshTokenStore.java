package com.gotcha.domain.auth.repository;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_KEY_PREFIX = "refresh_token:user:";

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidityMs;

    /**
     * 토큰 저장 (기존 토큰이 있으면 교체)
     */
    public void save(Long userId, String token) {
        Duration ttl = Duration.ofMillis(refreshTokenValidityMs);
        String userKey = USER_KEY_PREFIX + userId;

        String oldToken = redisTemplate.opsForValue().get(userKey);
        if (oldToken != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + oldToken);
        }

        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(userId), ttl);
        redisTemplate.opsForValue().set(userKey, token, ttl);
    }

    /**
     * 토큰으로 userId 조회
     */
    public Optional<Long> findUserIdByToken(String token) {
        String userId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (userId == null) {
            return Optional.empty();
        }
        return Optional.of(Long.parseLong(userId));
    }

    /**
     * logout (회원탈퇴 시 토큰 삭제)
     */
    public void deleteByUserId(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(userKey);
        if (token != null) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
        redisTemplate.delete(userKey);
    }
}
