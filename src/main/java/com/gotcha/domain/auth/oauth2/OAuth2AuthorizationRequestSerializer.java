package com.gotcha.domain.auth.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2AuthorizationRequest를 암호화/복호화하여 쿠키에 저장할 수 있게 변환하는 클래스.
 * AES-GCM 암호화를 사용하여 데이터 기밀성과 무결성을 보장합니다.
 */
@Slf4j
@Component
public class OAuth2AuthorizationRequestSerializer {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuth2AuthorizationRequestSerializer(
            ObjectMapper objectMapper,
            @Value("${oauth2.cookie-encryption-key}") String encryptionKey) {
        this.objectMapper = objectMapper;
        this.secretKey = createSecretKey(encryptionKey);
    }

    private SecretKey createSecretKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 키가 32바이트 미만이면 패딩
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        } else if (keyBytes.length > 32) {
            // 키가 32바이트 초과면 자르기
            byte[] truncatedKey = new byte[32];
            System.arraycopy(keyBytes, 0, truncatedKey, 0, 32);
            keyBytes = truncatedKey;
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * OAuth2AuthorizationRequest를 암호화된 문자열로 직렬화합니다.
     */
    public String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }
        try {
            Map<String, Object> data = toMap(authorizationRequest);
            String json = objectMapper.writeValueAsString(data);
            return encrypt(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OAuth2AuthorizationRequest", e);
            return null;
        }
    }

    /**
     * 암호화된 문자열을 OAuth2AuthorizationRequest로 역직렬화합니다.
     */
    public OAuth2AuthorizationRequest deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return null;
        }
        try {
            String json = decrypt(serialized);
            if (json == null) {
                return null;
            }
            Map<String, Object> data = objectMapper.readValue(json, new TypeReference<>() {});
            return fromMap(data);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize OAuth2AuthorizationRequest", e);
            return null;
        }
    }

    private Map<String, Object> toMap(OAuth2AuthorizationRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("authorizationUri", request.getAuthorizationUri());
        map.put("clientId", request.getClientId());
        map.put("redirectUri", request.getRedirectUri());
        map.put("scopes", request.getScopes());
        map.put("state", request.getState());
        map.put("additionalParameters", request.getAdditionalParameters());
        map.put("authorizationRequestUri", request.getAuthorizationRequestUri());
        map.put("attributes", request.getAttributes());
        return map;
    }

    @SuppressWarnings("unchecked")
    private OAuth2AuthorizationRequest fromMap(Map<String, Object> map) {
        // Jackson은 JSON 배열을 List로 역직렬화하므로 Set으로 변환 필요
        Object scopesObj = map.get("scopes");
        Set<String> scopes = scopesObj instanceof Collection
                ? new HashSet<>((Collection<String>) scopesObj)
                : new HashSet<>();

        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri((String) map.get("authorizationUri"))
                .clientId((String) map.get("clientId"))
                .redirectUri((String) map.get("redirectUri"))
                .scopes(scopes)
                .state((String) map.get("state"))
                .additionalParameters((Map<String, Object>) map.get("additionalParameters"))
                .attributes((Map<String, Object>) map.get("attributes"))
                .build();
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV + cipherText를 결합
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
}
