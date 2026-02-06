package com.gotcha._global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private boolean enabled = true;
    private int capacity = 100;
    private int refillTokens = 100;
    private int refillDurationSeconds = 60;
}
