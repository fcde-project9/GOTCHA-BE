package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import com.gotcha.domain.auth.service.OAuth2UserRegistrationService;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Set<String> SUPPORTED_PROVIDERS = Arrays.stream(SocialType.values())
            .map(type -> type.name().toLowerCase())
            .collect(Collectors.toSet());

    private final UserRepository userRepository;
    private final OAuth2UserRegistrationService registrationService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 지원하지 않는 소셜 타입 검증
        if (!SUPPORTED_PROVIDERS.contains(registrationId.toLowerCase())) {
            log.warn("Unsupported OAuth2 provider: {}", registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getCode(),
                            AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE.getMessage(), null));
        }

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        // 소셜 ID null 검증
        String socialId = userInfo.getId();
        if (socialId == null || socialId.isBlank()) {
            log.error("OAuth2 provider {} returned null or blank social ID", registrationId);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(AuthErrorCode.SOCIAL_LOGIN_FAILED.getCode(),
                            "소셜 로그인 정보를 가져올 수 없습니다", null));
        }

        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        boolean isNewUser = !userRepository.existsBySocialTypeAndSocialId(socialType, socialId);

        User user = userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .map(existingUser -> {
                    // 탈퇴한 사용자 로그인 차단
                    if (Boolean.TRUE.equals(existingUser.getIsDeleted())) {
                        log.warn("Deleted user attempted login - userId: {}, socialType: {}",
                                existingUser.getId(), socialType);
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(AuthErrorCode.USER_DELETED.getCode(),
                                        AuthErrorCode.USER_DELETED.getMessage(), null));
                    }
                    // 차단된 사용자 로그인 차단
                    if (existingUser.isBanned()) {
                        log.warn("Banned user attempted login - userId: {}, socialType: {}",
                                existingUser.getId(), socialType);
                        throw new OAuth2AuthenticationException(
                                new OAuth2Error(AuthErrorCode.USER_BANNED.getCode(),
                                        AuthErrorCode.USER_BANNED.getMessage(), null));
                    }
                    // 정지된 사용자: 기간 만료 시 자동 복구, 미만료 시 차단
                    if (existingUser.isSuspended()) {
                        if (!existingUser.checkAndRestoreIfSuspensionExpired()) {
                            log.warn("Suspended user attempted login - userId: {}, suspendedUntil: {}",
                                    existingUser.getId(), existingUser.getSuspendedUntil());
                            throw new OAuth2AuthenticationException(
                                    new OAuth2Error(AuthErrorCode.USER_SUSPENDED.getCode(),
                                            AuthErrorCode.USER_SUSPENDED.getMessage(), null));
                        }
                    }
                    return registrationService.updateExistingUser(existingUser, userInfo);
                })
                .orElseGet(() -> registrationService.createNewUser(userInfo, socialType));

        // 구글 로그인인 경우 access_token 저장 (탈퇴 시 연동 해제용)
        if (socialType == SocialType.GOOGLE) {
            String accessToken = userRequest.getAccessToken().getTokenValue();
            user.updateSocialRevokeToken(accessToken);
            log.debug("Google OAuth access token saved - userId: {}", user.getId());
        }

        return new CustomOAuth2User(user, oauth2User.getAttributes(), isNewUser);
    }
}
