package com.gotcha.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.user.dto.UserResponse;
import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("12345")
                .nickname("테스트유저")
                .profileImageUrl("https://example.com/profile.jpg")
                .build();
        setUserId(testUser, 1L);
    }

    private void setUserId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("현재 로그인한 사용자의 정보를 반환한다")
        void shouldReturnCurrentUserInfo() {
            // given
            when(securityUtil.getCurrentUser()).thenReturn(testUser);

            // when
            UserResponse result = userService.getMyInfo();

            // then
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nickname()).isEqualTo("테스트유저");
            assertThat(result.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
            assertThat(result.socialType()).isEqualTo(SocialType.KAKAO);
        }
    }
}
