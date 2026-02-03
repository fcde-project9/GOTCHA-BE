package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import com.gotcha.domain.user.entity.UserType;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final boolean isNewUser;

    public CustomOAuth2User(User user, Map<String, Object> attributes, boolean isNewUser) {
        this.user = user;
        this.attributes = attributes;
        this.isNewUser = isNewUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getNickname() {
        return user.getNickname();
    }

    public SocialType getSocialType() {
        return user.getSocialType();
    }

    public User getUser() {
        return user;
    }

    public boolean isNewUser() {
        return isNewUser;
    }

    public String getEmail() {
        return user.getEmail();
    }
}
