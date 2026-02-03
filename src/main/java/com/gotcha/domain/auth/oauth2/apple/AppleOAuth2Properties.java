package com.gotcha.domain.auth.oauth2.apple;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Apple OAuth2 설정 Properties.
 *
 * Apple Sign in을 위한 설정값들을 관리합니다.
 * - teamId: Apple Developer Team ID (10자리)
 * - keyId: Sign in with Apple Key ID (10자리)
 * - clientId: Service ID (Bundle ID)
 * - privateKey: .p8 파일 내용 (PEM 형식)
 * - tokenValidity: client_secret JWT 유효 시간 (기본 5분)
 */
@Component
@ConfigurationProperties(prefix = "apple.oauth2")
@Getter
@Setter
public class AppleOAuth2Properties {

    /**
     * Apple Developer Team ID.
     * Apple Developer 계정의 Team ID (10자리 영숫자)
     */
    private String teamId;

    /**
     * Sign in with Apple Key ID.
     * Apple Developer Console에서 생성한 Key의 ID (10자리 영숫자)
     */
    private String keyId;

    /**
     * Apple Service ID (Client ID).
     * Apple Developer Console에서 생성한 Service ID
     * 예: "com.gotcha.webapp"
     */
    private String clientId;

    /**
     * Apple Private Key.
     * .p8 파일의 전체 내용 (PEM 형식, BEGIN/END 포함)
     */
    private String privateKey;

    /**
     * client_secret JWT 유효 시간 (밀리초).
     * 기본값: 300000 (5분)
     * Apple은 최대 6개월까지 허용
     */
    private long tokenValidity = 300000L;
}
