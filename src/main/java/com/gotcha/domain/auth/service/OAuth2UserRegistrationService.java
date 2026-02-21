package com.gotcha.domain.auth.service;

import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OAuth2/OIDC 사용자 등록 공통 서비스.
 *
 * CustomOAuth2UserService와 CustomOidcUserService에서 공통으로 사용하는
 * 사용자 생성/업데이트 로직을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuth2UserRegistrationService {

    private static final String[] ADJECTIVES = {"빨간", "파란", "노란", "초록", "보라", "분홍", "하얀", "검은", "주황", "금색"};
    private static final String[] NOUNS = {"캡슐", "가챠", "뽑기", "별", "달", "구름", "토끼", "고양이", "강아지", "곰돌이"};
    private static final int MAX_NICKNAME_NUMBER = 10000;
    private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${user.default-profile-image-url}")
    private String defaultProfileImageUrl;

    /**
     * 신규 사용자를 생성합니다.
     *
     * @param userInfo   OAuth2/OIDC에서 추출한 사용자 정보
     * @param socialType 소셜 로그인 타입
     * @return 생성된 사용자
     */
    public User createNewUser(OAuth2UserInfo userInfo, SocialType socialType) {
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

    /**
     * 기존 사용자 정보를 업데이트합니다.
     *
     * @param user     기존 사용자
     * @param userInfo OAuth2/OIDC에서 추출한 사용자 정보
     * @return 업데이트된 사용자
     */
    public User updateExistingUser(User user, OAuth2UserInfo userInfo) {
        if (userInfo.getEmail() != null) {
            user.updateEmail(userInfo.getEmail());
        }
        user.updateLastLoginAt();
        return user;
    }

    /**
     * 랜덤 닉네임을 생성합니다.
     * "형용사 + 명사 + #숫자" 형식 (예: "빨간캡슐#1234")
     *
     * @return 유니크한 닉네임
     */
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

        // 최대 시도 횟수 초과 시 타임스탬프 기반 닉네임 생성
        String fallbackNickname = "가챠유저#" + System.currentTimeMillis() % 1000000;
        log.warn("Failed to generate unique nickname after {} attempts, using fallback: {}",
                MAX_NICKNAME_GENERATION_ATTEMPTS, fallbackNickname);
        return fallbackNickname;
    }
}
