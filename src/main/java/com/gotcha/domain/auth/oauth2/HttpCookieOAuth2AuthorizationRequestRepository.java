package com.gotcha.domain.auth.oauth2;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인가 요청을 쿠키 기반으로 저장하는 Repository.
 *
 * OAuth2AuthorizationRequest를 암호화하여 쿠키에 저장합니다.
 * 이 방식은 분산 환경(다중 인스턴스)에서도 안정적으로 동작합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String OAUTH2_AUTH_REQUEST_COOKIE_NAME = "oauth2_auth_request";
    private static final String REDIRECT_URI_COOKIE_NAME = "oauth2_redirect_uri";
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    private final OAuth2AuthorizationRequestSerializer serializer;

    @Value("${oauth2.allowed-redirect-uris:http://localhost:3000/oauth/callback}")
    private String allowedRedirectUrisString;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String serialized = getCookieValue(request, OAUTH2_AUTH_REQUEST_COOKIE_NAME);
        if (serialized == null) {
            log.debug("No authorization request cookie found");
            return null;
        }

        OAuth2AuthorizationRequest authRequest = serializer.deserialize(serialized);
        if (authRequest == null) {
            log.debug("Failed to deserialize authorization request from cookie");
        }
        return authRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookie(response);
            removeRedirectUriCookie(response);
            return;
        }

        String serialized = serializer.serialize(authorizationRequest);
        if (serialized == null) {
            log.error("Failed to serialize authorization request");
            return;
        }

        boolean isSecure = isSecureRequest(request);

        // 암호화된 OAuth2AuthorizationRequest를 쿠키에 저장
        // SameSite=None: Apple form_post (cross-site POST)에서 쿠키 전송을 위해 필요
        ResponseCookie authRequestCookie = ResponseCookie.from(OAUTH2_AUTH_REQUEST_COOKIE_NAME, serialized)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(COOKIE_EXPIRE_SECONDS)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, authRequestCookie.toString());

        // 프론트엔드에서 전달한 redirect_uri를 쿠키에 저장 (화이트리스트 검증)
        String redirectUri = request.getParameter("redirect_uri");
        if (redirectUri != null && !redirectUri.isBlank()) {
            if (isValidRedirectUri(redirectUri)) {
                String encodedUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                ResponseCookie redirectCookie = ResponseCookie.from(REDIRECT_URI_COOKIE_NAME, encodedUri)
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("None")
                        .maxAge(COOKIE_EXPIRE_SECONDS)
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, redirectCookie.toString());
                log.debug("Saved redirect_uri: {}", redirectUri);
            } else {
                log.warn("Invalid redirect_uri blocked: {}", redirectUri);
            }
        }

        log.debug("Saved authorization request to cookie with state: {}", authorizationRequest.getState());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = loadAuthorizationRequest(request);
        removeAuthorizationRequestCookie(response);
        removeRedirectUriCookie(response);

        if (authRequest != null) {
            log.debug("Removed authorization request with state: {}", authRequest.getState());
        }
        return authRequest;
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name)
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

    private void removeAuthorizationRequestCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH2_AUTH_REQUEST_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
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
                .flatMap(value -> {
                    try {
                        return Optional.of(URLDecoder.decode(value, StandardCharsets.UTF_8));
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid redirect_uri cookie value (decode failed)");
                        return Optional.empty();
                    }
                })
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
                .sameSite("None")
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

    /**
     * 요청이 HTTPS인지 확인 (프록시 뒤에서도 동작하도록 X-Forwarded-Proto 헤더 확인)
     */
    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }
}
