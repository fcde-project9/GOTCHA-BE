package com.gotcha.domain.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 쿠키 관련 유틸리티 클래스
 */
public final class CookieUtils {

    private static final String OAUTH2_STATE_COOKIE_NAME = "oauth2_auth_state";
    private static final String OAUTH2_REDIRECT_URI_COOKIE_NAME = "oauth2_redirect_uri";

    private CookieUtils() {
        // 유틸리티 클래스이므로 인스턴스화 방지
    }

    /**
     * 인증 관련 모든 쿠키 삭제 (탈퇴, 로그아웃 시 사용)
     *
     * @param request  HttpServletRequest (secure 여부 판단용)
     * @param response HttpServletResponse (쿠키 설정용)
     */
    public static void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = isSecureRequest(request);

        // OAuth2 상태 쿠키 삭제
        deleteCookie(response, OAUTH2_STATE_COOKIE_NAME, isSecure);

        // OAuth2 리다이렉트 URI 쿠키 삭제
        deleteCookie(response, OAUTH2_REDIRECT_URI_COOKIE_NAME, isSecure);
    }

    /**
     * 특정 쿠키 삭제
     */
    private static void deleteCookie(HttpServletResponse response, String cookieName, boolean isSecure) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .path("/")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite("Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * 요청이 HTTPS인지 확인 (프록시 뒤에서도 동작하도록 X-Forwarded-Proto 헤더 확인)
     */
    private static boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwardedProto);
    }
}
