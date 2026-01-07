package com.gotcha._global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String API_DESCRIPTION = """
            가챠샵 위치 정보 제공 서비스 API 문서

            ## 소셜 로그인

            아래 링크를 클릭하여 소셜 로그인을 진행할 수 있습니다.
            로그인 성공 시 프론트엔드 콜백 URL로 JWT 토큰이 전달됩니다.

            ### Local (localhost:8080)
            - [카카오 로그인](http://localhost:8080/oauth2/authorize/kakao)
            - [구글 로그인](http://localhost:8080/oauth2/authorize/google)
            - [네이버 로그인](http://localhost:8080/oauth2/authorize/naver)

            ### Dev (api.dev.gotcha.it.com)
            - [카카오 로그인](https://api.dev.gotcha.it.com/oauth2/authorize/kakao)
            - [구글 로그인](https://api.dev.gotcha.it.com/oauth2/authorize/google)
            - [네이버 로그인](https://api.dev.gotcha.it.com/oauth2/authorize/naver)
            """;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GOTCHA API")
                        .description(API_DESCRIPTION)
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("GOTCHA Team")
                                .email("fcdegotcha@gmail.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.dev.gotcha.it.com")
                                .description("Dev Server"),
                        new Server()
                                .url("https://api.gotcha.it.com")
                                .description("Production Server")
                ));
    }
}
