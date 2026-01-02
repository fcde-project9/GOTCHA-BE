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

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GOTCHA API")
                        .description("Gacha shop service platform API documentation")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("GOTCHA Team")
                                .email("fcdegotcha@gmail.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.dev.gotcha.it.com/api")
                                .description("Dev Server"),
                        new Server()
                                .url("https://api.gotcha.it.com/api")
                                .description("Production Server")
                ));
    }
}
