package com.gotcha._global.config;

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
                        // Public - 인증
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/**").permitAll()
                        // Public - OAuth2 로그인
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Public - 가게 조회
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/v3/api-docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                        // Authenticated - 사용자
                        .requestMatchers("/api/users/**").authenticated()
                        // Public - 가게 제보 (비회원도 가능)
                        .requestMatchers(HttpMethod.POST, "/api/shops/report").permitAll()
                        // Authenticated - 가게 관련 인증 필요 액션
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/favorite").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/comments/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/shops/*/reviews").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/shops/*/reviews/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/shops/*/reviews/*").authenticated()
                        // Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // TODO: 프로덕션 배포 전 .authenticated()로 변경 필수!
                        // 현재는 개발 편의상 permitAll() 사용 중
                        // 변경하지 않으면 새로 추가되는 API가 인증 없이 노출됨
                        .anyRequest().permitAll()
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
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
