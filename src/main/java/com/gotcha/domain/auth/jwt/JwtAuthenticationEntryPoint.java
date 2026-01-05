package com.gotcha.domain.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gotcha._global.common.ApiResponse;
import com.gotcha._global.exception.BusinessException;
import com.gotcha.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object authExceptionAttr = request.getAttribute("authException");

        AuthErrorCode errorCode;
        if (authExceptionAttr instanceof BusinessException businessException) {
            errorCode = (AuthErrorCode) businessException.getErrorCode();
        } else {
            errorCode = AuthErrorCode.UNAUTHORIZED;
        }

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> errorResponse = ApiResponse.error(errorCode);
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
