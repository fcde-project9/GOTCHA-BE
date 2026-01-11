package com.gotcha.domain.auth.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.exception.GlobalExceptionHandler;
import com.gotcha._global.util.SecurityUtil;
import com.gotcha.domain.auth.dto.ReissueRequest;
import com.gotcha.domain.auth.dto.TokenResponse;
import com.gotcha.domain.auth.exception.AuthException;
import com.gotcha.domain.auth.service.AuthService;
import com.gotcha.domain.user.entity.SocialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private SecurityUtil securityUtil;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/reissue")
    class Reissue {

        @Test
        @DisplayName("유효한 리프레시 토큰으로 새 토큰을 발급받는다")
        void shouldReissueTokensWithValidRefreshToken() throws Exception {
            // given
            ReissueRequest request = new ReissueRequest("valid-refresh-token");
            TokenResponse response = new TokenResponse(
                    "new-access-token",
                    "new-refresh-token",
                    new TokenResponse.UserResponse(1L, "테스트유저", "test@example.com", SocialType.KAKAO, false)
            );

            given(authService.reissueToken(anyString())).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                    .andExpect(jsonPath("$.data.user.id").value(1))
                    .andExpect(jsonPath("$.data.user.isNewUser").value(false));
        }

        @Test
        @DisplayName("존재하지 않는 리프레시 토큰이면 401 에러를 반환한다")
        void shouldReturn401WhenRefreshTokenNotFound() throws Exception {
            // given
            ReissueRequest request = new ReissueRequest("invalid-refresh-token");
            given(authService.reissueToken(anyString()))
                    .willThrow(AuthException.refreshTokenNotFound());

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A010"));
        }

        @Test
        @DisplayName("만료된 리프레시 토큰이면 401 에러를 반환한다")
        void shouldReturn401WhenRefreshTokenExpired() throws Exception {
            // given
            ReissueRequest request = new ReissueRequest("expired-refresh-token");
            given(authService.reissueToken(anyString()))
                    .willThrow(AuthException.refreshTokenExpired());

            // when & then
            mockMvc.perform(post("/api/auth/reissue")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A011"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @DisplayName("로그아웃에 성공하면 200을 반환한다")
        void shouldReturn200OnSuccessfulLogout() throws Exception {
            // given
            given(securityUtil.getCurrentUserId()).willReturn(1L);
            doNothing().when(authService).logout(anyLong());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("인증되지 않은 사용자가 로그아웃하면 401 에러를 반환한다")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // given
            given(securityUtil.getCurrentUserId())
                    .willThrow(AuthException.unauthorized());

            // when & then
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("A001"));
        }
    }
}
