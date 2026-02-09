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
 * Apple OAuth2 인가 요청에 response_mode=form_post 파라미터를 추가하는 리졸버.
 *
 * Apple은 scope에 name 또는 email이 포함되면 response_mode=form_post를 필수로 요구한다.
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
        return addResponseModeForApple(authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = defaultResolver.resolve(request, clientRegistrationId);
        return addResponseModeForApple(authRequest);
    }

    private OAuth2AuthorizationRequest addResponseModeForApple(OAuth2AuthorizationRequest request) {
        if (request == null) {
            return null;
        }
        if (!request.getAuthorizationUri().contains("appleid.apple.com")) {
            return request;
        }

        Map<String, Object> params = new LinkedHashMap<>(request.getAdditionalParameters());
        params.put("response_mode", "form_post");

        return OAuth2AuthorizationRequest.from(request)
                .additionalParameters(params)
                .build();
    }
}
