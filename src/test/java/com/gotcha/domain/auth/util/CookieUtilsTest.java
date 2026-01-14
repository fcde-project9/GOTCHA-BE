package com.gotcha.domain.auth.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CookieUtilsTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("clearAuthCookies")
    class ClearAuthCookies {

        @Test
        @DisplayName("HTTP 요청 시 인증 관련 쿠키를 삭제한다 (secure=false)")
        void shouldClearAuthCookiesForHttpRequest() {
            // given
            request.setScheme("http");
            request.setSecure(false);

            // when
            CookieUtils.clearAuthCookies(request, response);

            // then
            Collection<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(setCookieHeaders).hasSize(2);

            // oauth2_auth_state 쿠키 삭제 확인
            boolean hasStateCookie = setCookieHeaders.stream()
                    .anyMatch(cookie -> cookie.contains("oauth2_auth_state=")
                            && cookie.contains("Max-Age=0"));
            assertThat(hasStateCookie).isTrue();

            // oauth2_redirect_uri 쿠키 삭제 확인
            boolean hasRedirectCookie = setCookieHeaders.stream()
                    .anyMatch(cookie -> cookie.contains("oauth2_redirect_uri=")
                            && cookie.contains("Max-Age=0"));
            assertThat(hasRedirectCookie).isTrue();
        }

        @Test
        @DisplayName("HTTPS 요청 시 Secure 속성이 포함된 쿠키를 삭제한다")
        void shouldClearAuthCookiesWithSecureForHttpsRequest() {
            // given
            request.setScheme("https");
            request.setSecure(true);

            // when
            CookieUtils.clearAuthCookies(request, response);

            // then
            Collection<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
            assertThat(setCookieHeaders).hasSize(2);

            // Secure 속성 확인
            boolean allSecure = setCookieHeaders.stream()
                    .allMatch(cookie -> cookie.contains("Secure"));
            assertThat(allSecure).isTrue();
        }

        @Test
        @DisplayName("프록시 뒤에서 X-Forwarded-Proto 헤더로 HTTPS 판단")
        void shouldDetectHttpsFromForwardedProtoHeader() {
            // given
            request.setScheme("http");
            request.setSecure(false);
            request.addHeader("X-Forwarded-Proto", "https");

            // when
            CookieUtils.clearAuthCookies(request, response);

            // then
            Collection<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);

            // Secure 속성이 포함되어야 함
            boolean allSecure = setCookieHeaders.stream()
                    .allMatch(cookie -> cookie.contains("Secure"));
            assertThat(allSecure).isTrue();
        }

        @Test
        @DisplayName("쿠키에 HttpOnly, SameSite=Lax 속성이 포함된다")
        void shouldIncludeSecurityAttributesInCookies() {
            // given
            request.setSecure(true);

            // when
            CookieUtils.clearAuthCookies(request, response);

            // then
            Collection<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);

            boolean allHttpOnly = setCookieHeaders.stream()
                    .allMatch(cookie -> cookie.contains("HttpOnly"));
            assertThat(allHttpOnly).isTrue();

            boolean allSameSiteLax = setCookieHeaders.stream()
                    .allMatch(cookie -> cookie.contains("SameSite=Lax"));
            assertThat(allSameSiteLax).isTrue();
        }

        @Test
        @DisplayName("쿠키 경로가 /로 설정된다")
        void shouldSetCookiePathToRoot() {
            // when
            CookieUtils.clearAuthCookies(request, response);

            // then
            Collection<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);

            boolean allPathRoot = setCookieHeaders.stream()
                    .allMatch(cookie -> cookie.contains("Path=/"));
            assertThat(allPathRoot).isTrue();
        }
    }
}
