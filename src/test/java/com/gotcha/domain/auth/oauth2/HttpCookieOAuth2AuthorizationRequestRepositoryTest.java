package com.gotcha.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("HttpCookieOAuth2AuthorizationRequestRepository 테스트")
class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String TEST_ENCRYPTION_KEY = "test-oauth2-cookie-encryption-key-32chars";

    private HttpCookieOAuth2AuthorizationRequestRepository repository;
    private OAuth2AuthorizationRequestSerializer serializer;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        serializer = new OAuth2AuthorizationRequestSerializer(objectMapper, TEST_ENCRYPTION_KEY);
        repository = new HttpCookieOAuth2AuthorizationRequestRepository(serializer);
        ReflectionTestUtils.setField(repository, "allowedRedirectUrisString",
                "http://localhost:3000/oauth/callback,https://dev.gotcha.it.com/oauth/callback");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("OAuth2AuthorizationRequest 저장 및 로드")
    class SaveAndLoadAuthorizationRequest {

        @Test
        @DisplayName("저장 후 로드 - 동일한 요청 반환")
        void saveAndLoad_returnsIdenticalRequest() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            repository.saveAuthorizationRequest(authRequest, request, response);

            // 저장된 쿠키를 새 request에 설정
            String cookieHeader = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .filter(h -> h.contains("oauth2_auth_request"))
                    .findFirst()
                    .orElseThrow();
            String cookieValue = extractCookieValue(cookieHeader, "oauth2_auth_request");

            MockHttpServletRequest loadRequest = new MockHttpServletRequest();
            loadRequest.setCookies(
                    new jakarta.servlet.http.Cookie("oauth2_auth_request", cookieValue)
            );

            // when
            OAuth2AuthorizationRequest loadedRequest = repository.loadAuthorizationRequest(loadRequest);

            // then
            assertThat(loadedRequest).isNotNull();
            assertThat(loadedRequest.getState()).isEqualTo(authRequest.getState());
            assertThat(loadedRequest.getClientId()).isEqualTo(authRequest.getClientId());
            assertThat(loadedRequest.getRedirectUri()).isEqualTo(authRequest.getRedirectUri());
            assertThat(loadedRequest.getAuthorizationUri()).isEqualTo(authRequest.getAuthorizationUri());
        }

        @Test
        @DisplayName("쿠키 없음 - null 반환")
        void load_noCookie_returnsNull() {
            // when
            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("잘못된 쿠키 값 - null 반환")
        void load_invalidCookie_returnsNull() {
            // given
            request.setCookies(
                    new jakarta.servlet.http.Cookie("oauth2_auth_request", "invalid-encrypted-data")
            );

            // when
            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("화이트리스트 검증")
    class WhitelistValidation {

        @Test
        @DisplayName("화이트리스트에 포함된 URI - 유효함")
        void validRedirectUri_inWhitelist() {
            // given
            String validUri = "http://localhost:3000/oauth/callback";

            // when
            boolean result = repository.isValidRedirectUri(validUri);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("화이트리스트에 포함된 두 번째 URI - 유효함")
        void validRedirectUri_secondInWhitelist() {
            // given
            String validUri = "https://dev.gotcha.it.com/oauth/callback";

            // when
            boolean result = repository.isValidRedirectUri(validUri);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("화이트리스트에 없는 URI - 유효하지 않음")
        void invalidRedirectUri_notInWhitelist() {
            // given
            String invalidUri = "https://evil-site.com/callback";

            // when
            boolean result = repository.isValidRedirectUri(invalidUri);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null URI - 유효하지 않음")
        void invalidRedirectUri_null() {
            // when
            boolean result = repository.isValidRedirectUri(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 URI - 유효하지 않음")
        void invalidRedirectUri_blank() {
            // when
            boolean result = repository.isValidRedirectUri("   ");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("부분 일치하는 URI - 유효하지 않음 (정확히 일치해야 함)")
        void invalidRedirectUri_partialMatch() {
            // given - 화이트리스트: http://localhost:3000/oauth/callback
            String partialMatchUri = "http://localhost:3000/oauth/callback/extra";

            // when
            boolean result = repository.isValidRedirectUri(partialMatchUri);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("redirect_uri 쿠키 저장")
    class SaveRedirectUriCookie {

        @Test
        @DisplayName("유효한 redirect_uri - 쿠키에 저장됨")
        void validRedirectUri_savedToCookie() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            request.setParameter("redirect_uri", "http://localhost:3000/oauth/callback");

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasRedirectUriCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(h -> h.contains("oauth2_redirect_uri"));
            assertThat(hasRedirectUriCookie).isTrue();
        }

        @Test
        @DisplayName("유효하지 않은 redirect_uri - 쿠키에 저장되지 않음")
        void invalidRedirectUri_notSavedToCookie() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            request.setParameter("redirect_uri", "https://evil-site.com/callback");

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasRedirectUriCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(h -> h.contains("oauth2_redirect_uri") && !h.contains("Max-Age=0"));
            assertThat(hasRedirectUriCookie).isFalse();
        }

        @Test
        @DisplayName("redirect_uri 파라미터 없음 - 쿠키 저장 안 됨")
        void noRedirectUri_noCookieSaved() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasRedirectUriCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(h -> h.contains("oauth2_redirect_uri") && !h.contains("Max-Age=0"));
            assertThat(hasRedirectUriCookie).isFalse();
        }
    }

    @Nested
    @DisplayName("removeAuthorizationRequest 쿠키 정리")
    class RemoveAuthorizationRequest {

        @Test
        @DisplayName("removeAuthorizationRequest 호출 시 모든 OAuth2 쿠키 삭제됨")
        void removeAuthorizationRequest_removesAllOAuth2Cookies() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            request.setParameter("redirect_uri", "http://localhost:3000/oauth/callback");
            repository.saveAuthorizationRequest(authRequest, request, response);

            // 저장된 쿠키를 request에 설정
            String cookieHeader = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .filter(h -> h.contains("oauth2_auth_request"))
                    .findFirst()
                    .orElseThrow();
            String cookieValue = extractCookieValue(cookieHeader, "oauth2_auth_request");

            MockHttpServletRequest removeRequest = new MockHttpServletRequest();
            removeRequest.setCookies(
                    new jakarta.servlet.http.Cookie("oauth2_auth_request", cookieValue)
            );

            MockHttpServletResponse removeResponse = new MockHttpServletResponse();

            // when
            OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(removeRequest, removeResponse);

            // then
            assertThat(removed).isNotNull();
            assertThat(removed.getState()).isEqualTo(authRequest.getState());

            boolean hasAuthRequestDeleteCookie = removeResponse.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(h -> h.contains("oauth2_auth_request") && h.contains("Max-Age=0"));
            assertThat(hasAuthRequestDeleteCookie).isTrue();

            boolean hasRedirectUriDeleteCookie = removeResponse.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .anyMatch(h -> h.contains("oauth2_redirect_uri") && h.contains("Max-Age=0"));
            assertThat(hasRedirectUriDeleteCookie).isTrue();
        }
    }

    @Nested
    @DisplayName("redirect_uri 쿠키 읽기")
    class GetRedirectUriFromCookie {

        @Test
        @DisplayName("URL 인코딩된 쿠키에서 redirect_uri 읽기 - 디코딩 성공")
        void getRedirectUriFromCookie_urlDecoded() {
            // given
            String expectedUri = "http://localhost:3000/oauth/callback";
            String encodedUri = URLEncoder.encode(expectedUri, StandardCharsets.UTF_8);
            request.setCookies(
                    new jakarta.servlet.http.Cookie("oauth2_redirect_uri", encodedUri)
            );

            // when
            String result = repository.getRedirectUriFromCookie(request);

            // then
            assertThat(result).isEqualTo(expectedUri);
        }

        @Test
        @DisplayName("쿠키 없음 - null 반환")
        void getRedirectUriFromCookie_noCookie() {
            // when
            String result = repository.getRedirectUriFromCookie(request);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("화이트리스트 trim 처리")
    class WhitelistTrimProcessing {

        @Test
        @DisplayName("공백이 포함된 화이트리스트 - trim 처리 후 검증 성공")
        void whitelist_withSpaces_trimmedCorrectly() {
            // given
            ReflectionTestUtils.setField(repository, "allowedRedirectUrisString",
                    " http://localhost:3000/oauth/callback , https://dev.gotcha.it.com/oauth/callback ");

            // when & then
            assertThat(repository.isValidRedirectUri("http://localhost:3000/oauth/callback")).isTrue();
            assertThat(repository.isValidRedirectUri("https://dev.gotcha.it.com/oauth/callback")).isTrue();
        }

        @Test
        @DisplayName("빈 항목이 포함된 화이트리스트 - 빈 항목 무시됨")
        void whitelist_withEmptyEntries_ignored() {
            // given
            ReflectionTestUtils.setField(repository, "allowedRedirectUrisString",
                    "http://localhost:3000/oauth/callback,,https://dev.gotcha.it.com/oauth/callback");

            // when & then
            assertThat(repository.isValidRedirectUri("http://localhost:3000/oauth/callback")).isTrue();
            assertThat(repository.isValidRedirectUri("https://dev.gotcha.it.com/oauth/callback")).isTrue();
            assertThat(repository.isValidRedirectUri("")).isFalse();
        }
    }

    @Nested
    @DisplayName("쿠키 보안 설정")
    class CookieSecuritySettings {

        @Test
        @DisplayName("HTTPS 요청 시 Secure 쿠키 설정")
        void httpsRequest_secureCookieSet() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            request.setSecure(true);

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasSecureCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .filter(h -> h.contains("oauth2_auth_request"))
                    .anyMatch(h -> h.contains("Secure"));
            assertThat(hasSecureCookie).isTrue();
        }

        @Test
        @DisplayName("X-Forwarded-Proto: https 헤더 시 Secure 쿠키 설정")
        void forwardedProtoHttps_secureCookieSet() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
            request.addHeader("X-Forwarded-Proto", "https");

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasSecureCookie = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .filter(h -> h.contains("oauth2_auth_request"))
                    .anyMatch(h -> h.contains("Secure"));
            assertThat(hasSecureCookie).isTrue();
        }

        @Test
        @DisplayName("SameSite=Lax 설정 확인")
        void sameSiteLax_set() {
            // given
            OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();

            // when
            repository.saveAuthorizationRequest(authRequest, request, response);

            // then
            boolean hasSameSiteLax = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                    .filter(h -> h.contains("oauth2_auth_request"))
                    .anyMatch(h -> h.contains("SameSite=Lax"));
            assertThat(hasSameSiteLax).isTrue();
        }
    }

    private static final String FIXED_STATE = "test-state-12345";

    private OAuth2AuthorizationRequest createAuthorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/api/auth/callback/kakao")
                .state(FIXED_STATE)
                .build();
    }

    private String extractCookieValue(String cookieHeader, String cookieName) {
        // "oauth2_auth_request=value; Path=/; ..." 형태에서 value 추출
        String prefix = cookieName + "=";
        int startIndex = cookieHeader.indexOf(prefix) + prefix.length();
        int endIndex = cookieHeader.indexOf(";", startIndex);
        if (endIndex == -1) {
            endIndex = cookieHeader.length();
        }
        return cookieHeader.substring(startIndex, endIndex);
    }
}
