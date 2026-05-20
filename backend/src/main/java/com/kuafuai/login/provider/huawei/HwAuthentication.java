package com.kuafuai.login.provider.huawei;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class HwAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public HwAuthentication(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public HwAuthentication(Object principal, Collection<? extends GrantedAuthority> authorities) {
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
