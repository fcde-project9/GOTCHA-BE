package com.gotcha.domain.auth.oauth2.userinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OAuth2UserInfoTest {

    @Nested
    @DisplayName("KakaoOAuth2UserInfo")
    class KakaoOAuth2UserInfoTest {

        @Test
        @DisplayName("카카오 사용자 정보 추출 - 전체 정보 있는 경우")
        void extractKakaoUserInfo_withFullInfo_success() {
            // given
            Map<String, Object> profile = new HashMap<>();
            profile.put("nickname", "테스트유저");
            profile.put("profile_image_url", "https://k.kakaocdn.net/img.jpg");

            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", "test@kakao.com");
            kakaoAccount.put("profile", profile);

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 123456789L);
            attributes.put("kakao_account", kakaoAccount);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("123456789");
            assertThat(userInfo.getNickname()).isEqualTo("테스트유저");
            assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
            assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/img.jpg");
        }

        @Test
        @DisplayName("카카오 사용자 정보 추출 - kakao_account 없는 경우")
        void extractKakaoUserInfo_withoutKakaoAccount_returnsNull() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 123456789L);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("123456789");
            assertThat(userInfo.getNickname()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("카카오 사용자 정보 추출 - profile 없는 경우")
        void extractKakaoUserInfo_withoutProfile_returnsNullForProfileFields() {
            // given
            Map<String, Object> kakaoAccount = new HashMap<>();
            kakaoAccount.put("email", "test@kakao.com");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("id", 123456789L);
            attributes.put("kakao_account", kakaoAccount);

            // when
            KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("123456789");
            assertThat(userInfo.getNickname()).isNull();
            assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
            assertThat(userInfo.getProfileImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("GoogleOAuth2UserInfo")
    class GoogleOAuth2UserInfoTest {

        @Test
        @DisplayName("구글 사용자 정보 추출 - 전체 정보 있는 경우")
        void extractGoogleUserInfo_withFullInfo_success() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", "google-id-123");
            attributes.put("name", "구글유저");
            attributes.put("email", "test@gmail.com");
            attributes.put("picture", "https://lh3.googleusercontent.com/img.jpg");

            // when
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("google-id-123");
            assertThat(userInfo.getNickname()).isEqualTo("구글유저");
            assertThat(userInfo.getEmail()).isEqualTo("test@gmail.com");
            assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://lh3.googleusercontent.com/img.jpg");
        }

        @Test
        @DisplayName("구글 사용자 정보 추출 - 일부 정보 없는 경우")
        void extractGoogleUserInfo_withPartialInfo_returnsNullForMissingFields() {
            // given
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("sub", "google-id-123");

            // when
            GoogleOAuth2UserInfo userInfo = new GoogleOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("google-id-123");
            assertThat(userInfo.getNickname()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getProfileImageUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("NaverOAuth2UserInfo")
    class NaverOAuth2UserInfoTest {

        @Test
        @DisplayName("네이버 사용자 정보 추출 - 전체 정보 있는 경우")
        void extractNaverUserInfo_withFullInfo_success() {
            // given
            Map<String, Object> response = new HashMap<>();
            response.put("id", "naver-id-123");
            response.put("name", "네이버유저");
            response.put("email", "test@naver.com");
            response.put("profile_image", "https://phinf.pstatic.net/img.jpg");

            Map<String, Object> attributes = new HashMap<>();
            attributes.put("response", response);

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isEqualTo("naver-id-123");
            assertThat(userInfo.getNickname()).isEqualTo("네이버유저");
            assertThat(userInfo.getEmail()).isEqualTo("test@naver.com");
            assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://phinf.pstatic.net/img.jpg");
        }

        @Test
        @DisplayName("네이버 사용자 정보 추출 - response 없는 경우")
        void extractNaverUserInfo_withoutResponse_returnsNull() {
            // given
            Map<String, Object> attributes = new HashMap<>();

            // when
            NaverOAuth2UserInfo userInfo = new NaverOAuth2UserInfo(attributes);

            // then
            assertThat(userInfo.getId()).isNull();
            assertThat(userInfo.getNickname()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getProfileImageUrl()).isNull();
        }
    }
}
