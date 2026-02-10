package com.gotcha.domain.auth.oauth2;

import com.gotcha.domain.user.entity.SocialType;
import com.gotcha.domain.user.entity.User;
import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Apple OIDC 사용자 래퍼.
 *
 * OidcUser 인터페이스를 구현하여 Spring Security OIDC 인증과 호환됩니다.
 * 내부적으로 User 엔티티를 래핑하여 애플리케이션 도메인과 연결합니다.
 */
public class CustomOidcUser implements OidcUser {

    private final User user;
    private final OidcUser oidcUser;
    private final boolean isNewUser;

    public CustomOidcUser(User user, OidcUser oidcUser, boolean isNewUser) {
        this.user = user;
        this.oidcUser = oidcUser;
        this.isNewUser = isNewUser;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oidcUser.getAuthorities();
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }

    public User getUser() {
        return user;
    }

    public boolean isNewUser() {
        return isNewUser;
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

    public String getEmail() {
        return user.getEmail();
    }
}
