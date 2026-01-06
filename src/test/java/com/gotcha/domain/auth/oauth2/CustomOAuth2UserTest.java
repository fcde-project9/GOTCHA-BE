package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

class CustomOAuth2UserTest {

    @Test
    @DisplayName("CustomOAuth2User 생성 - 기본 정보 반환")
    void createCustomOAuth2User_returnsCorrectInfo() {
        // given
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저#1")
                .profileImageUrl("https://example.com/img.jpg")
                .isAnonymous(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);

        // when
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, attributes);

        // then
        assertThat(customOAuth2User.getUserId()).isEqualTo(1L);
        assertThat(customOAuth2User.getNickname()).isEqualTo("테스트유저#1");
        assertThat(customOAuth2User.getSocialType()).isEqualTo(SocialType.KAKAO);
        assertThat(customOAuth2User.getName()).isEqualTo("1");
        assertThat(customOAuth2User.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("CustomOAuth2User 권한 - ROLE_USER 권한 부여")
    void getAuthorities_returnsRoleUser() {
        // given
        User user = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("google-123")
                .nickname("구글유저#1")
                .isAnonymous(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Map<String, Object> attributes = new HashMap<>();

        // when
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, attributes);

        // then
        assertThat(customOAuth2User.getAuthorities())
                .hasSize(1)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("CustomOAuth2User attributes - OAuth2 attributes 반환")
    void getAttributes_returnsOAuth2Attributes() {
        // given
        User user = User.builder()
                .socialType(SocialType.NAVER)
                .socialId("naver-123")
                .nickname("네이버유저#1")
                .isAnonymous(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Map<String, Object> response = new HashMap<>();
        response.put("id", "naver-123");
        response.put("name", "네이버유저");

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("response", response);

        // when
        CustomOAuth2User customOAuth2User = new CustomOAuth2User(user, attributes);

        // then
        assertThat(customOAuth2User.getAttributes()).isEqualTo(attributes);
        assertThat(customOAuth2User.getAttributes()).containsKey("response");
    }
}
