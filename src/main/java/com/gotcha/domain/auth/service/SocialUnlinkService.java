package com.gotcha.domain.auth.service;

import com.gotcha.domain.auth.oauth2.apple.AppleClientSecretGenerator;
import com.gotcha.domain.auth.oauth2.apple.AppleOAuth2Properties;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 소셜 로그인 연결 끊기(unlink) 서비스.
 *
 * 회원 탈퇴 시 각 소셜 플랫폼의 앱 연결을 해제합니다.
 * - 카카오: Admin Key를 사용한 서버 방식 unlink
 * - 구글: 저장된 OAuth Access Token을 사용한 revoke
 * - 애플: 저장된 Refresh Token을 사용한 revoke
 * - 네이버: Access Token이 필요하여 현재 미지원 (로그만 기록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUnlinkService {

    private static final String KAKAO_UNLINK_PATH = "/v1/user/unlink";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    private final RestTemplate restTemplate;
    private final AppleClientSecretGenerator appleClientSecretGenerator;
    private final AppleOAuth2Properties appleOAuth2Properties;

    @Value("${kakao.api.admin-key}")
    private String kakaoAdminKey;

    @Value("${kakao.api.user-api-base-url}")
    private String kakaoUserApiBaseUrl;

    /**
     * 사용자의 소셜 연결 끊기
     *
     * @param user 탈퇴할 사용자
     */
    public void unlinkSocialAccount(User user) {
        SocialType socialType = user.getSocialType();
        String socialId = user.getSocialId();

        if (socialType == null || socialId == null) {
            log.info("No social account to unlink - userId: {}", user.getId());
            return;
        }

        switch (socialType) {
            case KAKAO -> unlinkKakao(user.getId(), socialId);
            case GOOGLE -> unlinkGoogle(user.getId(), user.getOauthAccessToken());
            case APPLE -> unlinkApple(user.getId(), user.getOauthAccessToken());
            case NAVER -> logUnsupportedUnlink(user.getId(), socialType);
        }
    }

    /**
     * 카카오 연결 끊기
     * POST https://kapi.kakao.com/v1/user/unlink
     */
    private void unlinkKakao(Long userId, String kakaoUserId) {
        log.info("Unlinking Kakao account - userId: {}, kakaoUserId: {}", userId, kakaoUserId);

        if (kakaoAdminKey == null || kakaoAdminKey.isBlank()) {
            log.warn("Kakao Admin Key is not configured - skipping unlink for userId: {}", userId);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("target_id_type", "user_id");
        body.add("target_id", kakaoUserId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            String unlinkUrl = kakaoUserApiBaseUrl + KAKAO_UNLINK_PATH;
            ResponseEntity<String> response = restTemplate.postForEntity(
                    unlinkUrl,
                    request,
                    String.class
            );
            log.info("Kakao unlink success - userId: {}, response: {}", userId, response.getBody());
        } catch (RestClientException e) {
            // 연결 끊기 실패해도 탈퇴는 계속 진행
            // 이미 연결이 끊긴 경우, 유효하지 않은 사용자 등
            log.warn("Kakao unlink failed - userId: {}, kakaoUserId: {}, error: {}",
                    userId, kakaoUserId, e.getMessage());
        }
    }

    /**
     * 구글 연결 끊기 (Token Revoke)
     * POST https://oauth2.googleapis.com/revoke
     */
    private void unlinkGoogle(Long userId, String oauthAccessToken) {
        log.info("Unlinking Google account - userId: {}", userId);

        if (oauthAccessToken == null || oauthAccessToken.isBlank()) {
            log.warn("Google OAuth access token is not available - skipping unlink for userId: {}", userId);
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", oauthAccessToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    GOOGLE_REVOKE_URL,
                    request,
                    String.class
            );
            log.info("Google unlink success - userId: {}, status: {}", userId, response.getStatusCode());
        } catch (RestClientException e) {
            // 연결 끊기 실패해도 탈퇴는 계속 진행
            // 토큰 만료, 이미 연결 끊긴 경우 등
            log.warn("Google unlink failed - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /**
     * 애플 연결 끊기 (Token Revoke)
     * POST https://appleid.apple.com/auth/revoke
     */
    private void unlinkApple(Long userId, String refreshToken) {
        log.info("Unlinking Apple account - userId: {}", userId);

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("Apple refresh token is not available - skipping unlink for userId: {}", userId);
            return;
        }

        try {
            String clientSecret = appleClientSecretGenerator.generateClientSecret();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", appleOAuth2Properties.getClientId());
            body.add("client_secret", clientSecret);
            body.add("token", refreshToken);
            body.add("token_type_hint", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    APPLE_REVOKE_URL,
                    request,
                    String.class
            );
            log.info("Apple unlink success - userId: {}, status: {}", userId, response.getStatusCode());
        } catch (RestClientException e) {
            // 연결 끊기 실패해도 탈퇴는 계속 진행
            // 토큰 만료, 이미 연결 끊긴 경우 등
            log.warn("Apple unlink failed - userId: {}, error: {}", userId, e.getMessage());
        } catch (Exception e) {
            // client_secret 생성 실패 등
            log.warn("Apple unlink failed (unexpected error) - userId: {}, error: {}", userId, e.getMessage());
        }
    }

    /**
     * 미지원 소셜 타입 로깅
     * 네이버는 Access Token이 필요하여 현재 서버에서 unlink 불가
     */
    private void logUnsupportedUnlink(Long userId, SocialType socialType) {
        log.info("Social unlink not supported on server - userId: {}, socialType: {}. "
                + "User should manually revoke access from {} account settings.",
                userId, socialType, socialType);
    }
}
