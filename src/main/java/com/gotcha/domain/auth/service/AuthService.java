package com.gotcha.domain.auth.service;

import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.oauth2.client.OAuth2Client;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String[] ADJECTIVES = {"빨간", "파란", "노란", "초록", "보라", "분홍", "하얀", "검은", "주황", "금색"};
    private static final String[] NOUNS = {"캡슐", "가챠", "뽑기", "별", "달", "구름", "토끼", "고양이", "강아지", "곰돌이"};
    private static final int MAX_NICKNAME_NUMBER = 10000;
    private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 10;

    private static final Set<String> SUPPORTED_PROVIDERS = Arrays.stream(SocialType.values())
            .map(type -> type.name().toLowerCase())
            .collect(Collectors.toSet());

    private final OAuth2Client oAuth2Client;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TokenResponse login(String provider, String accessToken) {
        validateProvider(provider);

        OAuth2UserInfo userInfo = oAuth2Client.getUserInfo(provider, accessToken);
        validateSocialId(userInfo.getId(), provider);

        SocialType socialType = SocialType.valueOf(provider.toUpperCase());

        boolean isNewUser = !userRepository.findBySocialTypeAndSocialId(socialType, userInfo.getId()).isPresent();

        User user = userRepository.findBySocialTypeAndSocialId(socialType, userInfo.getId())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo, socialType));

        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user);
        String jwtRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        log.info("User logged in: userId={}, provider={}, isNewUser={}", user.getId(), provider, isNewUser);

        return TokenResponse.of(jwtAccessToken, jwtRefreshToken, user, isNewUser);
    }

    private void validateProvider(String provider) {
        if (!SUPPORTED_PROVIDERS.contains(provider.toLowerCase())) {
            log.warn("Unsupported OAuth2 provider: {}", provider);
            throw AuthException.unsupportedSocialType();
        }
    }

    private void validateSocialId(String socialId, String provider) {
        if (socialId == null || socialId.isBlank()) {
            log.error("OAuth2 provider {} returned null or blank social ID", provider);
            throw AuthException.socialLoginFailed();
        }
    }

    private User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (userInfo.getProfileImageUrl() != null) {
            user.updateProfileImage(userInfo.getProfileImageUrl());
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
                .profileImageUrl(userInfo.getProfileImageUrl())
                .isAnonymous(false)
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

        String fallbackNickname = "가챠유저#" + System.currentTimeMillis() % 1000000;
        log.warn("Failed to generate unique nickname after {} attempts, using fallback: {}",
                MAX_NICKNAME_GENERATION_ATTEMPTS, fallbackNickname);
        return fallbackNickname;
    }
}
