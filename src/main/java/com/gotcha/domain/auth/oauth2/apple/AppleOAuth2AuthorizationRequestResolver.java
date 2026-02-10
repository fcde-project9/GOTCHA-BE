package com.gotcha.domain.auth.oauth2.apple;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Apple OAuth2 인가 요청을 커스터마이징하는 리졸버.
 *
 * - Apple은 scope에 name 또는 email이 포함되면 response_mode=form_post를 필수로 요구한다.
 * - Apple은 PKCE를 지원하지 않으므로 (openid-configuration에 code_challenge_methods_supported 없음)
 *   client-authentication-method=none으로 인해 Spring Security가 자동 첨부하는 PKCE 파라미터를 제거한다.
 */
@Component
public class AppleOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public AppleOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorize");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request);
        return customizeForApple(authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customizeForApple(authRequest);
    }

    private OAuth2AuthorizationRequest customizeForApple(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }
        if (!request.getAuthorizationUri().contains("appleid.apple.com")) {
            return request;
        }

        // additionalParameters에서 PKCE 파라미터 제거, response_mode 추가
        Map<String, Object> additionalParams = new LinkedHashMap<>(request.getAdditionalParameters());
        additionalParams.put("response_mode", "form_post");
        additionalParams.remove("code_challenge");
        additionalParams.remove("code_challenge_method");

        // attributes에서 code_verifier 제거 (토큰 요청 시 전송 방지)
        Map<String, Object> attributes = new LinkedHashMap<>(request.getAttributes());
        attributes.remove("code_verifier");

        // from(request)는 putAll로 병합하므로 PKCE 키 삭제 불가 → 새로 빌드
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(request.getAuthorizationUri())
                .clientId(request.getClientId())
                .redirectUri(request.getRedirectUri())
                .scopes(request.getScopes())
                .state(request.getState())
                .additionalParameters(additionalParams)
                .attributes(attrs -> attrs.putAll(attributes))
                .build();
    }
}
