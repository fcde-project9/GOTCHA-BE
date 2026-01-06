package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import com.gotcha.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.repository.UserRepository;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String[] ADJECTIVES = {"빨간", "파란", "노란", "초록", "보라", "분홍", "하얀", "검은", "주황", "금색"};
    private static final String[] NOUNS = {"캡슐", "가챠", "뽑기", "별", "달", "구름", "토끼", "고양이", "강아지", "곰돌이"};

    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oauth2User.getAttributes());

        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        User user = userRepository.findBySocialTypeAndSocialId(socialType, userInfo.getId())
                .map(existingUser -> updateExistingUser(existingUser, userInfo))
                .orElseGet(() -> createNewUser(userInfo, socialType));

        return new CustomOAuth2User(user, oauth2User.getAttributes());
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
        String nickname;
        do {
            String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
            String noun = NOUNS[random.nextInt(NOUNS.length)];
            int number = random.nextInt(100);
            nickname = adjective + noun + "#" + number;
        } while (userRepository.existsByNickname(nickname));

        return nickname;
    }
}
