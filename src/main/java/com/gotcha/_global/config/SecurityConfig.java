package com.gotcha._global.config;

import com.gotcha.domain.auth.jwt.JwtAuthenticationEntryPoint;
import com.gotcha.domain.auth.jwt.JwtAuthenticationFilter;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
                        // Public - 가게 조회
                        .requestMatchers(HttpMethod.GET, "/api/shops/**").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/api/v3/api-docs/**").permitAll()
                        // Authenticated - 사용자
                        .requestMatchers("/api/users/**").authenticated()
                        // Authenticated - 가게 관련 인증 필요 액션
                        .requestMatchers(HttpMethod.POST, "/api/shops/report").authenticated()
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
                        // 기타 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
