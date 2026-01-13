package com.gotcha.domain.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

/**
 * OAuth 로그인 완료 후 임시 토큰을 쿠키 기반으로 저장하는 서비스.
 *
 * 보안 강화:
 * - URL에 토큰 노출 대신 암호화된 쿠키 사용
 * - 30초 TTL로 자동 만료
 * - 토큰 교환 시 쿠키 즉시 삭제 (1회용)
 * - 분산 환경에서도 안정적으로 동작
 */
@Slf4j
@Service
public class OAuthTokenCookieService {

    private static final String OAUTH_TOKEN_COOKIE_NAME = "oauth2_token_data";
    private static final int CODE_EXPIRE_SECONDS = 30;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthTokenCookieService(
            ObjectMapper objectMapper,
            @Value("${oauth2.cookie-encryption-key}") String encryptionKey) {
        this.objectMapper = objectMapper;
        this.secretKey = createSecretKey(encryptionKey);
    }

    private SecretKey createSecretKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        } else if (keyBytes.length > 32) {
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            keyBytes = truncatedKey;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 토큰을 암호화된 쿠키에 저장하고 임시 코드 반환
     * 쿠키에 직접 저장하므로 HttpServletResponse 필요
     *
     * @param accessToken  액세스 토큰
     * @param refreshToken 리프레시 토큰
     * @param isNewUser    신규 사용자 여부
     * @param request      HTTP 요청 (보안 설정용)
     * @param response     HTTP 응답 (쿠키 저장용)
     * @return 임시 코드 (UUID, 프론트엔드 호환용)
     */
    public String storeTokens(String accessToken, String refreshToken, boolean isNewUser,
                              HttpServletRequest request, HttpServletResponse response) {
        String code = UUID.randomUUID().toString();
        TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);

        try {
            String encrypted = encrypt(objectMapper.writeValueAsString(tokenData));
            if (encrypted == null) {
                log.error("Failed to encrypt token data");
                return code;
            }

            boolean isSecure = isSecureRequest(request);
            ResponseCookie cookie = ResponseCookie.from(OAUTH_TOKEN_COOKIE_NAME, encrypted)
                    .path("/")
                    .httpOnly(true)
                    .secure(isSecure)
                    .sameSite("Lax")
                    .maxAge(CODE_EXPIRE_SECONDS)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            log.debug("Stored tokens in cookie with temp code: {}", code);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize token data", e);
        }

        return code;
    }

    /**
     * 쿠키에서 토큰 조회 및 삭제 (1회용)
     *
     * @param request  HTTP 요청 (쿠키 읽기)
     * @param response HTTP 응답 (쿠키 삭제)
     * @return 토큰 데이터 (없거나 만료된 경우 null)
     */
    public TokenData exchangeCode(HttpServletRequest request, HttpServletResponse response) {
        Optional<Cookie> cookieOpt = getCookie(request, OAUTH_TOKEN_COOKIE_NAME);
        if (cookieOpt.isEmpty()) {
            log.warn("No token cookie found");
            return null;
        }

        String encrypted = cookieOpt.get().getValue();
        String decrypted = decrypt(encrypted);
        if (decrypted == null) {
            log.warn("Failed to decrypt token cookie");
            removeTokenCookie(response);
            return null;
        }

        try {
            TokenData tokenData = objectMapper.readValue(decrypted, TokenData.class);
            removeTokenCookie(response);
            log.debug("Exchanged and removed token cookie");
            return tokenData;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize token data", e);
            removeTokenCookie(response);
            return null;
        }
    }

    /**
     * 테스트용: 토큰을 저장하고 암호화된 값 반환 (쿠키 없이)
     * local/dev 환경의 테스트 엔드포인트용
     */
    public String storeTokensForTest(String accessToken, String refreshToken, boolean isNewUser) {
        TokenData tokenData = new TokenData(accessToken, refreshToken, isNewUser);
        try {
            return encrypt(objectMapper.writeValueAsString(tokenData));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize token data for test", e);
            return null;
        }
    }

    private void removeTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH_TOKEN_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            log.error("Failed to encrypt data", e);
            return null;
        }
    }

    private String decrypt(String encryptedText) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(encryptedText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("Failed to decrypt data", e);
            return null;
        }
    }

    /**
     * 토큰 데이터를 담는 객체
     */
    public static class TokenData {
        private String accessToken;
        private String refreshToken;
        private boolean newUser;

        // Jackson 역직렬화용 기본 생성자
        public TokenData() {}

        public TokenData(String accessToken, String refreshToken, boolean isNewUser) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.newUser = isNewUser;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        /**
         * 신규 사용자 여부
         */
        public boolean isNewUser() {
            return newUser;
        }
    }
}
