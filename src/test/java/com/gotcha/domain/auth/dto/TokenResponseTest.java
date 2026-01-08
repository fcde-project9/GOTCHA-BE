package com.gotcha.domain.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TokenResponseTest {

    @Nested
    @DisplayName("TokenResponse 생성 테스트")
    class TokenResponseCreation {

        @Test
        @DisplayName("신규 사용자 - isNewUser true로 생성")
        void createTokenResponse_newUser_success() {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            // when
            TokenResponse response = TokenResponse.of(accessToken, refreshToken, user, true);

            // then
            assertThat(response.accessToken()).isEqualTo(accessToken);
            assertThat(response.refreshToken()).isEqualTo(refreshToken);
            assertThat(response.user().isNewUser()).isTrue();
            assertThat(response.user().id()).isEqualTo(1L);
            assertThat(response.user().nickname()).isEqualTo("테스트유저#1");
            assertThat(response.user().email()).isEqualTo("test@kakao.com");
            assertThat(response.user().socialType()).isEqualTo(SocialType.KAKAO);
        }

        @Test
        @DisplayName("기존 사용자 - isNewUser false로 생성")
        void createTokenResponse_existingUser_success() {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            // when
            TokenResponse response = TokenResponse.of(accessToken, refreshToken, user, false);

            // then
            assertThat(response.user().isNewUser()).isFalse();
            assertThat(response.user().socialType()).isEqualTo(SocialType.GOOGLE);
        }
    }

    @Nested
    @DisplayName("email 필드 테스트")
    class EmailFieldTest {

        @Test
        @DisplayName("email이 있는 경우 - email 포함")
        void emailExists_includesEmail() {
            // given
            User user = createTestUser(SocialType.NAVER, "test@naver.com");

            // when
            TokenResponse response = TokenResponse.of("token", "refresh", user, false);

            // then
            assertThat(response.user().email()).isEqualTo("test@naver.com");
        }

        @Test
        @DisplayName("email이 null인 경우 - null 포함")
        void emailNull_includesNull() {
            // given
            User user = createTestUser(SocialType.KAKAO, null);

            // when
            TokenResponse response = TokenResponse.of("token", "refresh", user, false);

            // then
            assertThat(response.user().email()).isNull();
        }
    }

    @Nested
    @DisplayName("소셜 타입별 테스트")
    class SocialTypeTest {

        @Test
        @DisplayName("카카오 사용자 - KAKAO 타입")
        void kakaoUser_kakaoSocialType() {
            // given
            User user = createTestUser(SocialType.KAKAO, "test@kakao.com");

            // when
            TokenResponse response = TokenResponse.of("token", "refresh", user, true);

            // then
            assertThat(response.user().socialType()).isEqualTo(SocialType.KAKAO);
        }

        @Test
        @DisplayName("구글 사용자 - GOOGLE 타입")
        void googleUser_googleSocialType() {
            // given
            User user = createTestUser(SocialType.GOOGLE, "test@gmail.com");

            // when
            TokenResponse response = TokenResponse.of("token", "refresh", user, true);

            // then
            assertThat(response.user().socialType()).isEqualTo(SocialType.GOOGLE);
        }

        @Test
        @DisplayName("네이버 사용자 - NAVER 타입")
        void naverUser_naverSocialType() {
            // given
            User user = createTestUser(SocialType.NAVER, "test@naver.com");

            // when
            TokenResponse response = TokenResponse.of("token", "refresh", user, true);

            // then
            assertThat(response.user().socialType()).isEqualTo(SocialType.NAVER);
        }
    }

    private User createTestUser(SocialType socialType, String email) {
        User user = User.builder()
                .socialType(socialType)
                .socialId("test-social-id")
                .nickname("테스트유저#1")
                .email(email)
                .isAnonymous(false)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }
}
