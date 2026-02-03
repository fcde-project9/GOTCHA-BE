package com.gotcha.domain.auth.oauth2.apple;

import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Apple OAuth2 client_secret JWT 생성기.
 *
 * Apple은 다른 OAuth2 제공자와 달리 client_secret으로 동적 생성된 JWT를 요구합니다.
 * - 알고리즘: ES256 (ECDSA using P-256 and SHA-256)
 * - 서명 키: Apple Developer Console에서 발급받은 .p8 Private Key
 * - 유효 기간: 최대 6개월 (권장: 5분)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleClientSecretGenerator {

    private static final String APPLE_AUTH_URL = "https://appleid.apple.com";

    private final AppleOAuth2Properties properties;

    /**
     * Apple OAuth2 인증에 사용할 client_secret JWT를 생성합니다.
     *
     * @return 서명된 JWT 문자열
     * @throws IllegalStateException JWT 생성 실패 시
     */
    public String generateClientSecret() {
        try {
            PrivateKey privateKey = getPrivateKey();
            Instant now = Instant.now();
            Instant expiration = now.plusMillis(properties.getTokenValidity());

            return Jwts.builder()
                    .header()
                    .keyId(properties.getKeyId())
                    .and()
                    .issuer(properties.getTeamId())
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(expiration))
                    .audience()
                    .add(APPLE_AUTH_URL)
                    .and()
                    .subject(properties.getClientId())
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
        } catch (Exception e) {
            log.error("Failed to generate Apple client secret", e);
            throw new IllegalStateException("Apple client_secret 생성 실패", e);
        }
    }

    /**
     * PEM 형식의 Private Key를 PrivateKey 객체로 변환합니다.
     */
    private PrivateKey getPrivateKey() throws Exception {
        String privateKeyContent = properties.getPrivateKey();

        if (privateKeyContent == null || privateKeyContent.isBlank()) {
            throw new IllegalStateException("Apple private key is not configured");
        }

        // PEM 헤더/푸터 제거 및 공백 제거
        privateKeyContent = privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(keySpec);
    }
}
