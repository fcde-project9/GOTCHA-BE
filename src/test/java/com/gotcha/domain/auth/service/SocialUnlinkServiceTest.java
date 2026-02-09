package com.gotcha.domain.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SocialUnlinkServiceTest {

    private static final String TEST_KAKAO_USER_API_BASE_URL = "https://kapi.kakao.com";
    private static final String KAKAO_UNLINK_URL = TEST_KAKAO_USER_API_BASE_URL + "/v1/user/unlink";
    private static final String GOOGLE_REVOKE_URL = "https://oauth2.googleapis.com/revoke";
    private static final String TEST_ADMIN_KEY = "test-admin-key";
    private static final String TEST_GOOGLE_ACCESS_TOKEN = "ya29.test-google-access-token";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SocialUnlinkService socialUnlinkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(socialUnlinkService, "kakaoAdminKey", TEST_ADMIN_KEY);
        ReflectionTestUtils.setField(socialUnlinkService, "kakaoUserApiBaseUrl", TEST_KAKAO_USER_API_BASE_URL);
    }

    private User createUser(SocialType socialType, String socialId) {
        return createUser(socialType, socialId, null);
    }

    private User createUser(SocialType socialType, String socialId, String socialRevokeToken) {
        User user = User.builder()
                .socialType(socialType)
                .socialId(socialId)
                .nickname("테스트유저")
                .build();
        setUserId(user, 1L);
        if (socialRevokeToken != null) {
            user.updateSocialRevokeToken(socialRevokeToken);
        }
        return user;
    }

    private void setUserId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("unlinkSocialAccount")
    class UnlinkSocialAccount {

        @Test
        @DisplayName("카카오 계정 연결 끊기 성공")
        void unlinkKakao_Success() {
            // given
            User kakaoUser = createUser(SocialType.KAKAO, "123456789");
            when(restTemplate.postForEntity(
                    eq(KAKAO_UNLINK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok("{\"id\": 123456789}"));

            // when
            socialUnlinkService.unlinkSocialAccount(kakaoUser);

            // then
            verify(restTemplate).postForEntity(
                    eq(KAKAO_UNLINK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("카카오 연결 끊기 실패해도 예외 발생하지 않음")
        void unlinkKakao_Failure_NoException() {
            // given
            User kakaoUser = createUser(SocialType.KAKAO, "123456789");
            when(restTemplate.postForEntity(
                    eq(KAKAO_UNLINK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("API Error"));

            // when - 예외 발생하지 않음
            socialUnlinkService.unlinkSocialAccount(kakaoUser);

            // then - API 호출은 시도됨
            verify(restTemplate).postForEntity(
                    eq(KAKAO_UNLINK_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("구글 계정 연결 끊기 성공")
        void unlinkGoogle_Success() {
            // given
            User googleUser = createUser(SocialType.GOOGLE, "google-123", TEST_GOOGLE_ACCESS_TOKEN);
            when(restTemplate.postForEntity(
                    eq(GOOGLE_REVOKE_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenReturn(ResponseEntity.ok(""));

            // when
            socialUnlinkService.unlinkSocialAccount(googleUser);

            // then
            verify(restTemplate).postForEntity(
                    eq(GOOGLE_REVOKE_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("구글 연결 끊기 실패해도 예외 발생하지 않음")
        void unlinkGoogle_Failure_NoException() {
            // given
            User googleUser = createUser(SocialType.GOOGLE, "google-123", TEST_GOOGLE_ACCESS_TOKEN);
            when(restTemplate.postForEntity(
                    eq(GOOGLE_REVOKE_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            )).thenThrow(new RestClientException("API Error"));

            // when - 예외 발생하지 않음
            socialUnlinkService.unlinkSocialAccount(googleUser);

            // then - API 호출은 시도됨
            verify(restTemplate).postForEntity(
                    eq(GOOGLE_REVOKE_URL),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }

        @Test
        @DisplayName("구글 OAuth 토큰이 null인 경우 연결 끊기 스킵")
        void unlinkGoogle_NullToken_Skip() {
            // given
            User googleUser = createUser(SocialType.GOOGLE, "google-123", null);

            // when
            socialUnlinkService.unlinkSocialAccount(googleUser);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("구글 OAuth 토큰이 빈 문자열인 경우 연결 끊기 스킵")
        void unlinkGoogle_EmptyToken_Skip() {
            // given
            User googleUser = createUser(SocialType.GOOGLE, "google-123", "");

            // when
            socialUnlinkService.unlinkSocialAccount(googleUser);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("네이버 계정은 서버에서 연결 끊기 미지원 (로그만 남김)")
        void unlinkNaver_NotSupported() {
            // given
            User naverUser = createUser(SocialType.NAVER, "naver-123");

            // when
            socialUnlinkService.unlinkSocialAccount(naverUser);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("소셜 타입이 null인 경우 연결 끊기 스킵")
        void unlinkSocialAccount_NullSocialType_Skip() {
            // given
            User user = User.builder()
                    .socialType(null)
                    .socialId(null)
                    .nickname("탈퇴한유저")
                    .build();
            setUserId(user, 1L);

            // when
            socialUnlinkService.unlinkSocialAccount(user);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("소셜 ID가 null인 경우 연결 끊기 스킵")
        void unlinkSocialAccount_NullSocialId_Skip() {
            // given
            User user = User.builder()
                    .socialType(SocialType.KAKAO)
                    .socialId(null)
                    .nickname("테스트유저")
                    .build();
            setUserId(user, 1L);

            // when
            socialUnlinkService.unlinkSocialAccount(user);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("Admin Key가 비어있는 경우 카카오 연결 끊기 스킵")
        void unlinkKakao_EmptyAdminKey_Skip() {
            // given
            ReflectionTestUtils.setField(socialUnlinkService, "kakaoAdminKey", "");
            User kakaoUser = createUser(SocialType.KAKAO, "123456789");

            // when
            socialUnlinkService.unlinkSocialAccount(kakaoUser);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }

        @Test
        @DisplayName("Admin Key가 null인 경우 카카오 연결 끊기 스킵")
        void unlinkKakao_NullAdminKey_Skip() {
            // given
            ReflectionTestUtils.setField(socialUnlinkService, "kakaoAdminKey", null);
            User kakaoUser = createUser(SocialType.KAKAO, "123456789");

            // when
            socialUnlinkService.unlinkSocialAccount(kakaoUser);

            // then - RestTemplate 호출 없음
            verify(restTemplate, never()).postForEntity(anyString(), any(), any());
        }
    }
}
