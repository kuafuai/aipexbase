package com.kuafuai.login.provider.wechat;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class WxWebAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public WxWebAuthentication(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public WxWebAuthentication(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}
