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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String[] ADJECTIVES = {"빨간", "파란", "노란", "초록", "보라", "분홍", "하얀", "검은", "주황", "금색"};
    private static final String[] NOUNS = {"캡슐", "가챠", "뽑기", "별", "달", "구름", "토끼", "고양이", "강아지", "곰돌이"};
    private static final int MAX_NICKNAME_NUMBER = 10000;
    private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 10;

    private final OAuth2Client oauth2Client;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public TokenResponse login(String provider, String accessToken) {
        // 1. 소셜 사용자 정보 조회
        OAuth2UserInfo userInfo = oauth2Client.getUserInfo(provider, accessToken);

        // 2. 소셜 ID 검증
        String socialId = userInfo.getId();
        if (socialId == null || socialId.isBlank()) {
            log.error("OAuth2 provider {} returned null or blank social ID", provider);
            throw AuthException.socialLoginFailed();
        }

        SocialType socialType = SocialType.valueOf(provider.toUpperCase());

        // 3. 사용자 조회 또는 생성
        boolean isNewUser = !userRepository.findBySocialTypeAndSocialId(socialType, socialId).isPresent();

        User user = userRepository.findBySocialTypeAndSocialId(socialType, socialId)
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo, socialType));

        // 4. JWT 토큰 발급
        String jwtAccessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return TokenResponse.of(jwtAccessToken, refreshToken, user, isNewUser);
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

        // 최대 시도 횟수 초과 시 UUID 기반 닉네임 생성
        String fallbackNickname = "가챠유저#" + System.currentTimeMillis() % 1000000;
        log.warn("Failed to generate unique nickname after {} attempts, using fallback: {}",
                MAX_NICKNAME_GENERATION_ATTEMPTS, fallbackNickname);
        return fallbackNickname;
    }
}
