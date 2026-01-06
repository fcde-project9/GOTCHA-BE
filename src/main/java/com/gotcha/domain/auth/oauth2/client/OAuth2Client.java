package com.gotcha.domain.auth.oauth2.client;

import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.oauth2.userinfo.GoogleOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.KakaoOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.NaverOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2Client {

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

    private final RestClient restClient = RestClient.create();

    public OAuth2UserInfo getUserInfo(String provider, String accessToken) {
        SocialType socialType = SocialType.valueOf(provider.toUpperCase());

        return switch (socialType) {
            case KAKAO -> getKakaoUserInfo(accessToken);
            case GOOGLE -> getGoogleUserInfo(accessToken);
            case NAVER -> getNaverUserInfo(accessToken);
        };
    }

    private OAuth2UserInfo getKakaoUserInfo(String accessToken) {
        try {
            Map<String, Object> attributes = restClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return new KakaoOAuth2UserInfo(attributes);
        } catch (Exception e) {
            log.error("Failed to get Kakao user info", e);
            throw AuthException.socialLoginFailed();
        }
    }

    private OAuth2UserInfo getGoogleUserInfo(String accessToken) {
        try {
            Map<String, Object> attributes = restClient.get()
                    .uri(GOOGLE_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return new GoogleOAuth2UserInfo(attributes);
        } catch (Exception e) {
            log.error("Failed to get Google user info", e);
            throw AuthException.socialLoginFailed();
        }
    }

    private OAuth2UserInfo getNaverUserInfo(String accessToken) {
        try {
            Map<String, Object> attributes = restClient.get()
                    .uri(NAVER_USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return new NaverOAuth2UserInfo(attributes);
        } catch (Exception e) {
            log.error("Failed to get Naver user info", e);
            throw AuthException.socialLoginFailed();
        }
    }
}
