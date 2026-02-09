package com.gotcha.domain.auth.oauth2.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AppleClientSecretGeneratorTest {

    private AppleOAuth2Properties properties;
    private AppleClientSecretGenerator generator;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트용 EC 키쌍 생성
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        keyGen.initialize(256);
        keyPair = keyGen.generateKeyPair();

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----";

        properties = new AppleOAuth2Properties();
        properties.setTeamId("TEAM123456");
        properties.setKeyId("KEY1234567");
        properties.setClientId("com.gotcha.webapp");
        properties.setPrivateKey(privateKeyPem);
        properties.setTokenValidity(300000L); // 5분

        generator = new AppleClientSecretGenerator(properties);
    }

    @Nested
    @DisplayName("client_secret JWT 생성")
    class GenerateClientSecretTest {

        @Test
        @DisplayName("JWT 생성 성공 - 3파트 구조")
        void generateClientSecret_success() {
            // when
            String clientSecret = generator.generateClientSecret();

            // then
            assertThat(clientSecret).isNotNull();
            assertThat(clientSecret.split("\\.")).hasSize(3); // JWT는 3파트
        }

        @Test
        @DisplayName("JWT claims 검증 - iss, sub, aud")
        void generateClientSecret_verifyClaims() {
            // when
            String clientSecret = generator.generateClientSecret();

            // then
            Claims claims = Jwts.parser()
                    .verifyWith(keyPair.getPublic())
                    .build()
                    .parseSignedClaims(clientSecret)
                    .getPayload();

            assertThat(claims.getIssuer()).isEqualTo("TEAM123456");
            assertThat(claims.getSubject()).isEqualTo("com.gotcha.webapp");

            // jjwt 0.12.x에서 audience는 Set<String>으로 반환
            Set<String> audience = claims.getAudience();
            assertThat(audience).contains("https://appleid.apple.com");
        }

        @Test
        @DisplayName("JWT 만료 시간 검증 - 약 5분 후")
        void generateClientSecret_verifyExpiration() {
            // when
            String clientSecret = generator.generateClientSecret();

            // then
            Claims claims = Jwts.parser()
                    .verifyWith(keyPair.getPublic())
                    .build()
                    .parseSignedClaims(clientSecret)
                    .getPayload();

            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            // 만료 시간이 발급 시간보다 약 5분(300초) 후
            long diffInSeconds = (expiration.getTime() - issuedAt.getTime()) / 1000;
            assertThat(diffInSeconds).isBetween(295L, 305L); // 5분 ± 5초 허용
        }

        @Test
        @DisplayName("JWT header 검증 - kid, alg")
        void generateClientSecret_verifyHeader() {
            // when
            String clientSecret = generator.generateClientSecret();

            // then
            String[] parts = clientSecret.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));

            assertThat(headerJson).contains("\"kid\":\"KEY1234567\"");
            assertThat(headerJson).contains("\"alg\":\"ES256\"");
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionTest {

        @Test
        @DisplayName("잘못된 private key - 예외 발생")
        void generateClientSecret_invalidPrivateKey_throwsException() {
            // given
            properties.setPrivateKey("invalid-key");
            generator = new AppleClientSecretGenerator(properties);

            // when & then
            assertThatThrownBy(() -> generator.generateClientSecret())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Apple client_secret 생성 실패");
        }

        @Test
        @DisplayName("빈 private key - 예외 발생")
        void generateClientSecret_emptyPrivateKey_throwsException() {
            // given
            properties.setPrivateKey("");
            generator = new AppleClientSecretGenerator(properties);

            // when & then
            assertThatThrownBy(() -> generator.generateClientSecret())
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
