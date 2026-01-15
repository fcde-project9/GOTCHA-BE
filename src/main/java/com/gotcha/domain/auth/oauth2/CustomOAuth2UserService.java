package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.exception.AuthErrorCode;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.security.SecureRandom;
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

    private static final String[] ADJECTIVES = {"빨간", "파란", "노란", "초록", "보라", "분홍", "하얀", "검은", "주황", "금색"};
    private static final String[] NOUNS = {"캡슐", "가챠", "뽑기", "별", "달", "구름", "토끼", "고양이", "강아지", "곰돌이"};
    private static final int MAX_NICKNAME_NUMBER = 10000;
    private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 10;

    private static final Set<String> SUPPORTED_PROVIDERS = Arrays.stream(SocialType.values())
            .map(type -> type.name().toLowerCase())
            .collect(Collectors.toSet());

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @org.springframework.beans.factory.annotation.Value("${user.default-profile-image-url}")
    private String defaultProfileImageUrl;

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
                    return updateExistingUser(existingUser, userInfo);
                })
                .orElseGet(() -> createNewUser(userInfo, socialType));

        // 구글 로그인인 경우 OAuth2 access_token 저장 (탈퇴 시 연동 해제용)
        if (socialType == SocialType.GOOGLE) {
            String oauthAccessToken = userRequest.getAccessToken().getTokenValue();
            user.updateOAuthAccessToken(oauthAccessToken);
            log.debug("Google OAuth access token saved - userId: {}", user.getId());
        }

        return new CustomOAuth2User(user, oauth2User.getAttributes(), isNewUser);
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (userInfo.getEmail() != null) {
            user.updateEmail(userInfo.getEmail());
        }
        user.updateLastLoginAt();
        return user;
    }

    private User createNewUser(OAuth2UserInfo userInfo, SocialType socialType) {
        String nickname = generateRandomNickname();

        User user = User.builder()
                .socialType(socialType)
                .socialId(userInfo.getId())
                .nickname(nickname)
                .email(userInfo.getEmail())
                .profileImageUrl(defaultProfileImageUrl)
                .build();

        user.updateLastLoginAt();
        return userRepository.save(user);
    }

    private String generateRandomNickname() {
        for (int attempt = 0; attempt < MAX_NICKNAME_GENERATION_ATTEMPTS; attempt++) {
            String adjective = ADJECTIVES[secureRandom.nextInt(ADJECTIVES.length)];
            String noun = NOUNS[secureRandom.nextInt(NOUNS.length)];
            int number = secureRandom.nextInt(MAX_NICKNAME_NUMBER);
            String nickname = adjective + noun + "#" + number;

            if (!userRepository.existsByNickname(nickname)) {
                return nickname;
            }
        }

        // 최대 시도 횟수 초과 시 UUID 기반 닉네임 생성
        String fallbackNickname = "가챠유저#" + System.currentTimeMillis() % 1000000;
        log.warn("Failed to generate unique nickname after {} attempts, using fallback: {}",
                MAX_NICKNAME_GENERATION_ATTEMPTS, fallbackNickname);
        return fallbackNickname;
    }
}
