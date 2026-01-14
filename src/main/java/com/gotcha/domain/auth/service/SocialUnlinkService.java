package com.gotcha.domain.auth.service;

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
 * - 구글/네이버: Access Token이 필요하여 현재 미지원 (로그만 기록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUnlinkService {

    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    private final RestTemplate restTemplate;

    @Value("${kakao.api.admin-key}")
    private String kakaoAdminKey;

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
            case GOOGLE -> logUnsupportedUnlink(user.getId(), socialType);
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
            ResponseEntity<String> response = restTemplate.postForEntity(
                    KAKAO_UNLINK_URL,
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
     * 미지원 소셜 타입 로깅
     * 구글/네이버는 Access Token이 필요하여 현재 서버에서 unlink 불가
     */
    private void logUnsupportedUnlink(Long userId, SocialType socialType) {
        log.info("Social unlink not supported on server - userId: {}, socialType: {}. "
                + "User should manually revoke access from {} account settings.",
                userId, socialType, socialType);
    }
}
