package com.gotcha.domain.auth.oauth2.userinfo;

import java.util.Map;

/**
 * Apple Sign in 사용자 정보 추출 클래스.
 *
 * Apple OIDC는 user-info endpoint가 없고, ID Token (JWT)에서 사용자 정보를 추출합니다.
 * - sub: 사용자 고유 식별자
 * - email: 이메일 (Private Relay 가능)
 * - user.name: 최초 로그인 시에만 제공되는 사용자 이름
 */
public class AppleOAuth2UserInfo extends OAuth2UserInfo {

    public AppleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    /**
     * Apple 사용자 고유 ID (sub claim).
     * 형식: "000000.abcdef123456.0000"
     */
    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    /**
     * 사용자 닉네임.
     * Apple은 최초 로그인 시에만 user.name을 제공합니다.
     * 재로그인 시에는 null 반환.
     */
    @Override
    public String getNickname() {
        Object user = attributes.get("user");
        if (user instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> userMap = (Map<String, Object>) user;
            Object name = userMap.get("name");
            if (name instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> nameMap = (Map<String, String>) name;
                String firstName = nameMap.get("firstName");
                String lastName = nameMap.get("lastName");
                return buildFullName(lastName, firstName);
            }
        }
        return null;
    }

    private String buildFullName(String lastName, String firstName) {
        StringBuilder sb = new StringBuilder();
        if (lastName != null && !lastName.isBlank()) {
            sb.append(lastName);
        }
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName);
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    /**
     * 사용자 이메일.
     * Apple은 Private Relay 이메일을 제공할 수 있습니다.
     * 예: "abc123@privaterelay.appleid.com"
     */
    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    /**
     * 프로필 이미지 URL.
     * Apple은 프로필 이미지를 제공하지 않습니다.
     */
    @Override
    public String getProfileImageUrl() {
        return null;
    }
}
