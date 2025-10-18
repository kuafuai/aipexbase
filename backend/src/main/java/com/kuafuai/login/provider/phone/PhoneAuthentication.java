package com.kuafuai.login.provider.phone;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PhoneAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public PhoneAuthentication(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public PhoneAuthentication(Object principal, Collection<? extends GrantedAuthority> authorities) {
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
