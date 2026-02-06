package com.gotcha._global.config;

import com.gotcha._global.filter.RateLimitFilter;
import com.gotcha.domain.auth.jwt.JwtAuthenticationEntryPoint;
import com.gotcha.domain.auth.jwt.JwtAuthenticationFilter;
import com.gotcha.domain.auth.oauth2.CustomOAuth2UserService;
import com.gotcha.domain.auth.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.gotcha.domain.auth.oauth2.OAuth2AuthenticationFailureHandler;
import com.gotcha.domain.auth.oauth2.OAuth2AuthenticationSuccessHandler;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RateLimitFilter rateLimitFilter;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // prometheus - 설정
                        .requestMatchers("/actuator/**").permitAll()
                        // CORS preflight (OPTIONS) must always be allowed
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public - 인증
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/**").permitAll()
                        // Public - OAuth2 로그인
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Authenticated - 가게 수정/삭제 (ADMIN 전용, 서비스 레이어에서 권한 체크)
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/shops/*/main-image").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*").authenticated()
                        // Public - 가게 조회
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/v3/api-docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                        // Dev API (local/dev 환경에서만 빈 등록됨)
                        .requestMatchers("/api/dev/**").permitAll()
                        // Authenticated - 사용자
                        .requestMatchers("/api/users/**").authenticated()
                        // Public - 가게 생성/제보 (비회원도 가능)
                        .requestMatchers(HttpMethod.POST, "/api/shops/save").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/shops/report").permitAll()
                        // Public - file upload (used by reviews, reports, etc.)
                        .requestMatchers(HttpMethod.POST, "/api/files/**").permitAll()
                        // Authenticated - 가게 관련 인증 필요 액션
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/reviews/*").authenticated()
                        // Authenticated - 리뷰 좋아요
                        .requestMatchers(HttpMethod.POST, "/api/shops/reviews/*/like").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/reviews/*/like").authenticated()
                        // Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 그 외는 전부 인증 필요
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization ->
                                authorization.baseUri("/oauth2/authorize")
                                        .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                        .redirectionEndpoint(redirection ->
                                redirection.baseUri("/api/auth/callback/*"))
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, jwtAuthenticationFilter.getClass())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .collect(Collectors.toList())
        );
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // allowCredentials(true)와 함께 사용할 때는 명시적으로 헤더 지정
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);  // preflight 캐시 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
