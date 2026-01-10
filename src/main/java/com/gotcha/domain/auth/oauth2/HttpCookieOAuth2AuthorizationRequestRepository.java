package com.gotcha.domain.auth.oauth2;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인가 요청을 메모리(Caffeine Cache) + 쿠키 기반으로 저장하는 Repository.
 *
 * 쿠키에는 state 값만 저장하고, 실제 OAuth2AuthorizationRequest는
 * Caffeine Cache에 저장하여 직렬화 문제를 회피하고 TTL로 메모리 누수를 방지합니다.
 */
@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_STATE_COOKIE_NAME = "oauth2_auth_state";
    private static final String REDIRECT_URI_COOKIE_NAME = "oauth2_redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 120;

    @Value("${oauth2.allowed-redirect-uris:http://localhost:3000/oauth/callback}")
    private String allowedRedirectUrisString;

    // state -> OAuth2AuthorizationRequest 매핑 (TTL 기반 캐시)
    private final Cache<String, OAuth2AuthorizationRequest> authorizationRequests = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(COOKIE_EXPIRE_SECONDS))
            .maximumSize(10000)
            .build();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = getStateFromCookie(request);
        if (state == null) {
            log.debug("No state cookie found");
            return null;
        }

        OAuth2AuthorizationRequest authRequest = authorizationRequests.getIfPresent(state);
        if (authRequest == null) {
            log.debug("No authorization request found for state: {}", state);
        }
        return authRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookie(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        authorizationRequests.put(state, authorizationRequest);

        ResponseCookie stateCookie = ResponseCookie.from(OAUTH2_STATE_COOKIE_NAME, state)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(COOKIE_EXPIRE_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, stateCookie.toString());

        // 프론트엔드에서 전달한 redirect_uri를 쿠키에 저장 (화이트리스트 검증)
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isBlank()) {
            if (isValidRedirectUri(redirectUri)) {
                String encodedUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                ResponseCookie redirectCookie = ResponseCookie.from(REDIRECT_URI_COOKIE_NAME, encodedUri)
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .maxAge(COOKIE_EXPIRE_SECONDS)
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, redirectCookie.toString());
                log.debug("Saved redirect_uri: {}", redirectUri);
            } else {
                log.warn("Invalid redirect_uri blocked: {}", redirectUri);
            }
        }

        log.debug("Saved authorization request with state: {}", state);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        String state = getStateFromCookie(request);
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest authRequest = authorizationRequests.getIfPresent(state);
        authorizationRequests.invalidate(state);
        removeAuthorizationRequestCookie(request, response);
        removeRedirectUriCookie(response);

        log.debug("Removed authorization request with state: {}", state);
        return authRequest;
    }

    private String getStateFromCookie(HttpServletRequest request) {
        return getCookie(request, OAUTH2_STATE_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);
    }

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    private void removeAuthorizationRequestCookie(HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH2_STATE_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 쿠키에서 프론트엔드가 전달한 redirect_uri를 읽음 (URL 디코딩 적용)
     */
    public String getRedirectUriFromCookie(HttpServletRequest request) {
        return getCookie(request, REDIRECT_URI_COOKIE_NAME)
                .map(Cookie::getValue)
                .map(value -> URLDecoder.decode(value, StandardCharsets.UTF_8))
                .orElse(null);
    }

    /**
     * redirect_uri 쿠키 삭제
     */
    public void removeRedirectUriCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REDIRECT_URI_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * redirect_uri가 화이트리스트에 포함되어 있는지 검증
     */
    boolean isValidRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            return false;
        }
        return getAllowedRedirectUris().stream()
                .anyMatch(redirectUri::equals);
    }

    private List<String> getAllowedRedirectUris() {
        return Arrays.stream(allowedRedirectUrisString.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isEmpty())
                .toList();
    }
}
