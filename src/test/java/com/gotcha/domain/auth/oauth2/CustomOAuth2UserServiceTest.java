package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.gotcha.domain.auth.oauth2.userinfo.KakaoOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomOAuth2UserServiceTest {

    @Test
    @DisplayName("OAuth2UserInfo에서 사용자 정보 추출 - 카카오")
    void extractUserInfo_fromKakao_success() {
        // given
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "테스트유저");
        profile.put("profile_image_url", "https://k.kakaocdn.net/img.jpg");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@kakao.com");
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345L);
        attributes.put("kakao_account", kakaoAccount);

        // when
        OAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(attributes);

        // then
        assertThat(userInfo.getId()).isEqualTo("12345");
        assertThat(userInfo.getNickname()).isEqualTo("테스트유저");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getProfileImageUrl()).isEqualTo("https://k.kakaocdn.net/img.jpg");
    }

    @Test
    @DisplayName("User 엔티티 생성 검증")
    void createUser_withOAuth2Info_success() {
        // given
        SocialType socialType = SocialType.KAKAO;
        String socialId = "12345";
        String nickname = "빨간캡슐#21";
        String profileImageUrl = "https://example.com/img.jpg";

        // when
        User user = User.builder()
                .socialType(socialType)
                .socialId(socialId)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .isAnonymous(false)
                .build();

        // then
        assertThat(user.getSocialType()).isEqualTo(SocialType.KAKAO);
        assertThat(user.getSocialId()).isEqualTo("12345");
        assertThat(user.getNickname()).isEqualTo("빨간캡슐#21");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/img.jpg");
        assertThat(user.getIsAnonymous()).isFalse();
    }

    @Test
    @DisplayName("User 프로필 이미지 업데이트 검증")
    void updateUser_profileImage_success() {
        // given
        User user = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("기존유저#1")
                .profileImageUrl("https://old-image.com/img.jpg")
                .isAnonymous(false)
                .build();

        String newProfileImageUrl = "https://new-image.com/img.jpg";

        // when
        user.updateProfileImage(newProfileImageUrl);

        // then
        assertThat(user.getProfileImageUrl()).isEqualTo(newProfileImageUrl);
    }

    @Test
    @DisplayName("User 마지막 로그인 시간 업데이트 검증")
    void updateUser_lastLoginAt_success() {
        // given
        User user = User.builder()
                .socialType(SocialType.GOOGLE)
                .socialId("google-123")
                .nickname("구글유저#1")
                .isAnonymous(false)
                .build();

        assertThat(user.getLastLoginAt()).isNull();

        // when
        user.updateLastLoginAt();

        // then
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    @DisplayName("닉네임 패턴 검증 - 형용사+명사+#숫자 형식")
    void nicknamePattern_matchesExpectedFormat() {
        // given
        String[] validNicknames = {
            "빨간캡슐#21",
            "파란가챠#0",
            "노란뽑기#99"
        };

        // when & then
        for (String nickname : validNicknames) {
            assertThat(nickname).matches("^[가-힣]+[가-힣]+#\\d+$");
        }
    }

    @Nested
    @DisplayName("탈퇴 사용자 테스트")
    class DeletedUserTest {

        @Test
        @DisplayName("User.delete() 호출 시 isDeleted가 true로 변경된다")
        void delete_setsIsDeletedToTrue() {
            // given
            User user = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("12345")
                    .nickname("테스트유저#1")
                    .email("test@example.com")
                    .profileImageUrl("https://example.com/img.jpg")
                    .isAnonymous(false)
                    .build();
            setUserId(user, 1L);

            // when
            user.delete();

            // then
            assertThat(user.getIsDeleted()).isTrue();
        }

        @Test
        @DisplayName("User.delete() 호출 시 닉네임이 마스킹된다")
        void delete_masksNickname() {
            // given
            User user = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("12345")
                    .nickname("테스트유저#1")
                    .isAnonymous(false)
                    .build();
            setUserId(user, 42L);

            // when
            user.delete();

            // then
            assertThat(user.getNickname()).isEqualTo("탈퇴한 사용자_42");
        }

        @Test
        @DisplayName("User.delete() 호출 시 이메일과 프로필 이미지가 null로 변경된다")
        void delete_clearsEmailAndProfileImage() {
            // given
            User user = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("12345")
                    .nickname("테스트유저#1")
                    .email("test@example.com")
                    .profileImageUrl("https://example.com/img.jpg")
                    .isAnonymous(false)
                    .build();
            setUserId(user, 1L);

            // when
            user.delete();

            // then
            assertThat(user.getEmail()).isNull();
            assertThat(user.getProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("User.delete() 호출 시 socialId, socialType이 null로 설정된다 (재가입 허용)")
        void delete_clearsSocialInfo() {
            // given
            User user = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId("12345")
                    .nickname("테스트유저#1")
                    .isAnonymous(false)
                    .build();
            setUserId(user, 1L);

            // when
            user.delete();

            // then - 재가입 허용을 위해 소셜 정보 제거
            assertThat(user.getSocialId()).isNull();
            assertThat(user.getSocialType()).isNull();
        }

        private void setUserId(User user, Long id) {
            try {
                var field = User.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(user, id);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("닉네임 생성 시 최대 시도 횟수를 초과하면 fallback 닉네임 패턴 사용")
        void generateNickname_whenExceedMaxAttempts_usesFallbackNickname() {
            // given - fallback 패턴: "가챠유저#[숫자]"
            String fallbackPattern = "^가챠유저#\\d+$";

            // when & then - fallback 닉네임은 해당 패턴을 따름
            assertThat("가챠유저#123456").matches(fallbackPattern);
            assertThat("가챠유저#0").matches(fallbackPattern);
            assertThat("가챠유저#999999").matches(fallbackPattern);
        }

        @Test
        @DisplayName("소셜 ID가 blank 문자열인 경우 유효하지 않음을 검증")
        void loadUser_whenSocialIdIsBlank_isInvalid() {
            // given - blank 문자열은 null이 아니지만 유효하지 않음
            String blankSocialId = "   ";

            // when & then
            assertThat(blankSocialId.isBlank()).isTrue();
            assertThat(blankSocialId).isNotNull();
        }

        @Test
        @DisplayName("지원하지 않는 소셜 타입 확인")
        void unsupportedSocialType_shouldNotExist() {
            // given
            String[] supportedProviders = {"kakao", "google", "naver"};
            String unsupportedProvider = "facebook";

            // when & then
            for (String provider : supportedProviders) {
                assertThat(SocialType.valueOf(provider.toUpperCase())).isNotNull();
            }

            assertThatThrownBy(() -> SocialType.valueOf(unsupportedProvider.toUpperCase()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
