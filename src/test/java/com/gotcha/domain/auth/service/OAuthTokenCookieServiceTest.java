package com.gotcha.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha.domain.auth.service.OAuthTokenCookieService.TokenData;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuthTokenCookieServiceTest {

    private OAuthTokenCookieService cacheService;
    private ObjectMapper objectMapper;
    private static final String TEST_ENCRYPTION_KEY = "test-encryption-key-for-unit-testing-32bytes";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cacheService = new OAuthTokenCookieService(objectMapper, TEST_ENCRYPTION_KEY);
    }

    @Nested
    @DisplayName("storeTokens")
    class StoreTokens {

        @Test
        @DisplayName("토큰을 쿠키에 저장하고 임시 코드를 반환한다")
        void shouldStoreTokensInCookieAndReturnCode() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            boolean isNewUser = true;
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            String code = cacheService.storeTokens(accessToken, refreshToken, isNewUser, request, response);

            // then
            assertThat(code).isNotNull();
            assertThat(code).isNotBlank();
            // 쿠키가 설정되었는지 확인
            assertThat(response.getHeader("Set-Cookie")).isNotNull();
            assertThat(response.getHeader("Set-Cookie")).contains("oauth2_token_data=");
        }

        @Test
        @DisplayName("매번 다른 임시 코드를 반환한다")
        void shouldReturnDifferentCodesEachTime() {
            // given
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            MockHttpServletRequest request2 = new MockHttpServletRequest();
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            // when
            String code1 = cacheService.storeTokens(accessToken, refreshToken, true, request1, response1);
            String code2 = cacheService.storeTokens(accessToken, refreshToken, true, request2, response2);

            // then
            assertThat(code1).isNotEqualTo(code2);
        }

        @Test
        @DisplayName("HTTPS 요청 시 Secure 쿠키를 설정한다")
        void shouldSetSecureCookieForHttps() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setSecure(true);
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cacheService.storeTokens("access", "refresh", true, request, response);

            // then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Secure");
        }

        @Test
        @DisplayName("X-Forwarded-Proto가 https면 Secure 쿠키를 설정한다")
        void shouldSetSecureCookieForProxyHttps() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Forwarded-Proto", "https");
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cacheService.storeTokens("access", "refresh", true, request, response);

            // then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Secure");
        }
    }

    @Nested
    @DisplayName("exchangeCode")
    class ExchangeCode {

        @Test
        @DisplayName("유효한 쿠키로 토큰을 조회한다")
        void shouldReturnTokensWithValidCookie() {
            // given - 먼저 토큰 저장
            String accessToken = "test-access-token";
            String refreshToken = "test-refresh-token";
            boolean isNewUser = true;

            MockHttpServletRequest storeRequest = new MockHttpServletRequest();
            MockHttpServletResponse storeResponse = new MockHttpServletResponse();
            cacheService.storeTokens(accessToken, refreshToken, isNewUser, storeRequest, storeResponse);

            // 저장된 쿠키 값 추출
            String setCookieHeader = storeResponse.getHeader("Set-Cookie");
            String cookieValue = extractCookieValue(setCookieHeader, "oauth2_token_data");

            // 교환 요청 준비
            MockHttpServletRequest exchangeRequest = new MockHttpServletRequest();
            exchangeRequest.setCookies(new Cookie("oauth2_token_data", cookieValue));
            MockHttpServletResponse exchangeResponse = new MockHttpServletResponse();

            // when
            TokenData tokenData = cacheService.exchangeCode(exchangeRequest, exchangeResponse);

            // then
            assertThat(tokenData).isNotNull();
            assertThat(tokenData.getAccessToken()).isEqualTo(accessToken);
            assertThat(tokenData.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(tokenData.isNewUser()).isTrue();
        }

        @Test
        @DisplayName("쿠키 없으면 null을 반환한다")
        void shouldReturnNullWhenNoCookie() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            TokenData tokenData = cacheService.exchangeCode(request, response);

            // then
            assertThat(tokenData).isNull();
        }

        @Test
        @DisplayName("잘못된 쿠키 값이면 null을 반환한다")
        void shouldReturnNullWithInvalidCookieValue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("oauth2_token_data", "invalid-encrypted-value"));
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            TokenData tokenData = cacheService.exchangeCode(request, response);

            // then
            assertThat(tokenData).isNull();
            // 잘못된 쿠키는 삭제됨
            assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
        }

        @Test
        @DisplayName("토큰 교환 후 쿠키가 삭제된다")
        void shouldRemoveCookieAfterExchange() {
            // given
            MockHttpServletRequest storeRequest = new MockHttpServletRequest();
            MockHttpServletResponse storeResponse = new MockHttpServletResponse();
            cacheService.storeTokens("access", "refresh", true, storeRequest, storeResponse);

            String cookieValue = extractCookieValue(storeResponse.getHeader("Set-Cookie"), "oauth2_token_data");

            MockHttpServletRequest exchangeRequest = new MockHttpServletRequest();
            exchangeRequest.setCookies(new Cookie("oauth2_token_data", cookieValue));
            MockHttpServletResponse exchangeResponse = new MockHttpServletResponse();

            // when
            cacheService.exchangeCode(exchangeRequest, exchangeResponse);

            // then - 쿠키 삭제 확인 (Max-Age=0)
            String deleteCookie = exchangeResponse.getHeader("Set-Cookie");
            assertThat(deleteCookie).contains("Max-Age=0");
        }

        @Test
        @DisplayName("isNewUser가 false인 경우도 정상 처리")
        void shouldHandleExistingUser() {
            // given
            MockHttpServletRequest storeRequest = new MockHttpServletRequest();
            MockHttpServletResponse storeResponse = new MockHttpServletResponse();
            cacheService.storeTokens("access", "refresh", false, storeRequest, storeResponse);

            String cookieValue = extractCookieValue(storeResponse.getHeader("Set-Cookie"), "oauth2_token_data");

            MockHttpServletRequest exchangeRequest = new MockHttpServletRequest();
            exchangeRequest.setCookies(new Cookie("oauth2_token_data", cookieValue));
            MockHttpServletResponse exchangeResponse = new MockHttpServletResponse();

            // when
            TokenData tokenData = cacheService.exchangeCode(exchangeRequest, exchangeResponse);

            // then
            assertThat(tokenData).isNotNull();
            assertThat(tokenData.isNewUser()).isFalse();
        }
    }

    @Nested
    @DisplayName("storeTokensForTest")
    class StoreTokensForTest {

        @Test
        @DisplayName("테스트용 토큰 저장 - 암호화된 값 반환")
        void shouldReturnEncryptedTokenData() {
            // given & when
            String encrypted = cacheService.storeTokensForTest("access", "refresh", true);

            // then
            assertThat(encrypted).isNotNull();
            assertThat(encrypted).isNotBlank();
        }

        @Test
        @DisplayName("테스트용 암호화된 값으로 토큰 교환 가능")
        void shouldBeExchangeableWithEncryptedValue() {
            // given
            String encrypted = cacheService.storeTokensForTest("test-access", "test-refresh", true);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setCookies(new Cookie("oauth2_token_data", encrypted));
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            TokenData tokenData = cacheService.exchangeCode(request, response);

            // then
            assertThat(tokenData).isNotNull();
            assertThat(tokenData.getAccessToken()).isEqualTo("test-access");
            assertThat(tokenData.getRefreshToken()).isEqualTo("test-refresh");
            assertThat(tokenData.isNewUser()).isTrue();
        }
    }

    @Nested
    @DisplayName("쿠키 보안 설정")
    class CookieSecurity {

        @Test
        @DisplayName("쿠키에 HttpOnly 설정이 있다")
        void shouldHaveHttpOnlyFlag() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cacheService.storeTokens("access", "refresh", true, request, response);

            // then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("HttpOnly");
        }

        @Test
        @DisplayName("쿠키에 SameSite=Lax 설정이 있다")
        void shouldHaveSameSiteLax() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cacheService.storeTokens("access", "refresh", true, request, response);

            // then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("SameSite=Lax");
        }

        @Test
        @DisplayName("쿠키 만료 시간이 30초다")
        void shouldHave30SecondMaxAge() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            MockHttpServletResponse response = new MockHttpServletResponse();

            // when
            cacheService.storeTokens("access", "refresh", true, request, response);

            // then
            String setCookie = response.getHeader("Set-Cookie");
            assertThat(setCookie).contains("Max-Age=30");
        }
    }

    /**
     * Set-Cookie 헤더에서 특정 쿠키의 값을 추출
     */
    private String extractCookieValue(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null) {
            return null;
        }
        // "oauth2_token_data=VALUE; Path=/; ..." 형식에서 VALUE 추출
        String prefix = cookieName + "=";
        int startIndex = setCookieHeader.indexOf(prefix);
        if (startIndex == -1) {
            return null;
        }
        startIndex += prefix.length();
        int endIndex = setCookieHeader.indexOf(";", startIndex);
        if (endIndex == -1) {
            endIndex = setCookieHeader.length();
        }
        return setCookieHeader.substring(startIndex, endIndex);
    }
}
