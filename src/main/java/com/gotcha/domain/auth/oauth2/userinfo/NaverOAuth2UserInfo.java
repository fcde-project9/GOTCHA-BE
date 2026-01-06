package com.gotcha.domain.auth.oauth2.userinfo;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        Object id = response.get("id");
        return id instanceof String ? (String) id : (id != null ? String.valueOf(id) : null);
    }

    @Override
    public String getNickname() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        Object name = response.get("name");
        return name instanceof String ? (String) name : null;
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        Object email = response.get("email");
        return email instanceof String ? (String) email : null;
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> response = getResponse();
        if (response == null) {
            return null;
        }
        Object profileImage = response.get("profile_image");
        return profileImage instanceof String ? (String) profileImage : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getResponse() {
        return (Map<String, Object>) attributes.get("response");
    }
}
