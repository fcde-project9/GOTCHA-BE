package com.gotcha.domain.auth.oauth2.userinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gotcha.domain.user.entity.SocialType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoFactoryTest {

    @Test
    @DisplayName("OAuth2UserInfo 생성 - 카카오")
    void getOAuth2UserInfo_kakao_returnsKakaoOAuth2UserInfo() {
        // given
        String registrationId = "kakao";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        // when
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        // then
        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
        assertThat(userInfo.getId()).isEqualTo("12345");
    }

    @Test
    @DisplayName("OAuth2UserInfo 생성 - 구글")
    void getOAuth2UserInfo_google_returnsGoogleOAuth2UserInfo() {
        // given
        String registrationId = "google";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "google-sub-123");

        // when
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        // then
        assertThat(userInfo).isInstanceOf(GoogleOAuth2UserInfo.class);
        assertThat(userInfo.getId()).isEqualTo("google-sub-123");
    }

    @Test
    @DisplayName("OAuth2UserInfo 생성 - 네이버")
    void getOAuth2UserInfo_naver_returnsNaverOAuth2UserInfo() {
        // given
        String registrationId = "naver";
        Map<String, Object> response = new HashMap<>();
        response.put("id", "naver-id-123");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        // when
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes);

        // then
        assertThat(userInfo).isInstanceOf(NaverOAuth2UserInfo.class);
        assertThat(userInfo.getId()).isEqualTo("naver-id-123");
    }

    @Test
    @DisplayName("OAuth2UserInfo 생성 - SocialType 파라미터 (카카오)")
    void getOAuth2UserInfo_bySocialType_kakao_returnsKakaoOAuth2UserInfo() {
        // given
        SocialType socialType = SocialType.KAKAO;
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        // when
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(socialType, attributes);

        // then
        assertThat(userInfo).isInstanceOf(KakaoOAuth2UserInfo.class);
    }

    @Test
    @DisplayName("OAuth2UserInfo 생성 - 지원하지 않는 provider")
    void getOAuth2UserInfo_unsupportedProvider_throwsException() {
        // given
        String registrationId = "unsupported";
        Map<String, Object> attributes = new HashMap<>();

        // when & then
        assertThatThrownBy(() -> OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, attributes))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
