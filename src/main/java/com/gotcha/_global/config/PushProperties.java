package com.gotcha._global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "push.vapid")
@Getter
@Setter
public class PushProperties {
    private String publicKey;
    private String privateKey;
    private String subject;
}
