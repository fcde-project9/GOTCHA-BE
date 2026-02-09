package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import com.gotcha.domain.auth.oauth2.userinfo.AppleOAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.auth.service.OAuth2UserRegistrationService;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Apple OIDC 사용자 정보 처리 서비스.
 *
 * Apple Sign in은 OIDC 프로토콜을 사용하므로 OidcUserService를 상속합니다.
 * ID Token(JWT)에서 사용자 정보를 추출하고, User 엔티티를 생성/조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final OAuth2UserRegistrationService registrationService;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.debug("OIDC login attempt from provider: {}", registrationId);

        // OIDC는 현재 Apple만 지원
        if (!"apple".equalsIgnoreCase(registrationId)) {
            log.warn("Unsupported OIDC provider: {}", registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getCode(),
                            AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getMessage(), null));
        }

        // ID Token claims를 attributes로 사용
        Map<String, Object> attributes = new HashMap<>(oidcUser.getClaims());

        // Apple 최초 로그인 시 user 파라미터가 additionalParameters에 포함될 수 있음
        Map<String, Object> additionalParams = userRequest.getAdditionalParameters();
        if (additionalParams.containsKey("user")) {
            attributes.put("user", additionalParams.get("user"));
            log.debug("Apple user info from additional parameters included");
        }

        OAuth2UserInfo userInfo = new AppleOAuth2UserInfo(attributes);

        // 소셜 ID (sub claim) 검증
        String socialId = userInfo.getId();
        if (socialId == null || socialId.isBlank()) {
            log.error("Apple OIDC returned null or blank social ID (sub claim)");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode(),
                            "Apple 로그인 정보를 가져올 수 없습니다", null));
        }

        SocialType socialType = SocialType.APPLE;
        boolean isNewUser = !userRepository.existsBySocialTypeAndSocialId(socialType, socialId);

        User user = userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .map(existingUser -> {
                    // 탈퇴한 사용자 로그인 차단
                    if (Boolean.TRUE.equals(existingUser.getIsDeleted())) {
                        log.warn("Deleted user attempted Apple login - userId: {}", existingUser.getId());
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(AuthErrorCode.USER_DELETED.getCode(),
                                        AuthErrorCode.USER_DELETED.getMessage(), null));
                    }
                    return registrationService.updateExistingUser(existingUser, userInfo);
                })
                .orElseGet(() -> registrationService.createNewUser(userInfo, socialType));

        // Apple refresh_token 저장 (탈퇴 시 연동 해제용)
        // Apple은 token 응답에서 refresh_token을 제공함
        String refreshToken = (String) userRequest.getAdditionalParameters().get("refresh_token");
        if (refreshToken != null) {
            user.updateSocialRevokeToken(refreshToken);
            log.debug("Apple refresh token saved - userId: {}", user.getId());
        }

        return new CustomOidcUser(user, oidcUser, isNewUser);
    }
}
