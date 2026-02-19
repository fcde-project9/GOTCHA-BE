package com.gotcha._global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "push")
@Getter
@Setter
public class PushProperties {

    private Vapid vapid = new Vapid();
    private Apns apns = new Apns();

    @Getter
    @Setter
    public static class Vapid {
        private String publicKey;
        private String privateKey;
        private String subject;
    }

    @Getter
    @Setter
    public static class Apns {
        private String teamId;
        private String keyId;
        private String privateKey;
        private String bundleId;
        private boolean production;
    }
}
