package com.gotcha.domain.auth.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha.domain.auth.dto.LoginRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.jwt.JwtTokenProvider;
import com.gotcha.domain.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Nested
    @DisplayName("POST /api/auth/login/{provider}")
    class LoginTest {

        @Test
        @DisplayName("카카오 로그인 성공 - 신규 회원")
        void login_kakaoNewUser_success() throws Exception {
            // given
            String provider = "kakao";
            String accessToken = "valid-kakao-access-token";
            LoginRequest request = new LoginRequest(accessToken);

            TokenResponse tokenResponse = createTokenResponse(
                    1L, "빨간캡슐#21", "https://example.com/profile.jpg", true
            );

            given(authService.login(provider, accessToken)).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("jwt-refresh-token"))
                    .andExpect(jsonPath("$.data.user.id").value(1))
                    .andExpect(jsonPath("$.data.user.nickname").value("빨간캡슐#21"))
                    .andExpect(jsonPath("$.data.user.isNewUser").value(true));
        }

        @Test
        @DisplayName("구글 로그인 성공 - 기존 회원")
        void login_googleExistingUser_success() throws Exception {
            // given
            String provider = "google";
            String accessToken = "valid-google-access-token";
            LoginRequest request = new LoginRequest(accessToken);

            TokenResponse tokenResponse = createTokenResponse(
                    2L, "파란가챠#99", "https://example.com/profile.jpg", false
            );

            given(authService.login(provider, accessToken)).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.user.isNewUser").value(false));
        }

        @Test
        @DisplayName("네이버 로그인 성공")
        void login_naver_success() throws Exception {
            // given
            String provider = "naver";
            String accessToken = "valid-naver-access-token";
            LoginRequest request = new LoginRequest(accessToken);

            TokenResponse tokenResponse = createTokenResponse(
                    3L, "노란뽑기#42", "https://example.com/profile.jpg", true
            );

            given(authService.login(provider, accessToken)).willReturn(tokenResponse);

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.refreshToken").exists());
        }

        @Test
        @DisplayName("지원하지 않는 소셜 로그인 제공자 - 실패")
        void login_unsupportedProvider_badRequest() throws Exception {
            // given
            String provider = "facebook";
            String accessToken = "some-access-token";
            LoginRequest request = new LoginRequest(accessToken);

            given(authService.login(provider, accessToken))
                    .willThrow(AuthException.unsupportedSocialType());

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A006"));
        }

        @Test
        @DisplayName("유효하지 않은 소셜 accessToken - 인증 실패")
        void login_invalidAccessToken_unauthorized() throws Exception {
            // given
            String provider = "kakao";
            String accessToken = "invalid-access-token";
            LoginRequest request = new LoginRequest(accessToken);

            given(authService.login(provider, accessToken))
                    .willThrow(AuthException.socialLoginFailed());

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A005"));
        }

        @Test
        @DisplayName("accessToken이 빈 문자열인 경우 - 유효성 검증 실패")
        void login_emptyAccessToken_badRequest() throws Exception {
            // given
            String provider = "kakao";
            LoginRequest request = new LoginRequest("");

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("accessToken이 null인 경우 - 유효성 검증 실패")
        void login_nullAccessToken_badRequest() throws Exception {
            // given
            String provider = "kakao";
            String requestBody = "{}";

            // when & then
            mockMvc.perform(post("/api/auth/login/{provider}", provider)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }
    }

    private TokenResponse createTokenResponse(Long userId, String nickname, String profileImageUrl, boolean isNewUser) {
        TokenResponse.UserResponse userResponse = new TokenResponse.UserResponse(
                userId,
                nickname,
                profileImageUrl,
                isNewUser
        );
        return new TokenResponse("jwt-access-token", "jwt-refresh-token", userResponse);
    }
}
