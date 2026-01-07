package com.gotcha.domain.auth.oauth2.client;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import com.gotcha.domain.user.entity.SocialType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Client {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestTemplate restTemplate;

    public OAuth2UserInfo getUserInfo(String provider, String accessToken) {
        SocialType socialType = parseSocialType(provider);
        String userInfoUrl = getUserInfoUrl(socialType);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> attributes = response.getBody();
            if (attributes == null || attributes.isEmpty()) {
                log.error("Empty response from {} user info API", provider);
                throw AuthException.socialLoginFailed();
            }

            return OAuth2UserInfoFactory.getOAuth2UserInfo(socialType, attributes);

        } catch (RestClientException e) {
            log.error("Failed to get user info from {}: {}", provider, e.getMessage());
            throw AuthException.socialLoginFailed();
        }
    }

    private SocialType parseSocialType(String provider) {
        try {
            return SocialType.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Unsupported social type: {}", provider);
            throw AuthException.unsupportedSocialType();
        }
    }

    private String getUserInfoUrl(SocialType socialType) {
        return switch (socialType) {
            case KAKAO -> KAKAO_USER_INFO_URL;
            case GOOGLE -> GOOGLE_USER_INFO_URL;
            case NAVER -> NAVER_USER_INFO_URL;
        };
    }
}
