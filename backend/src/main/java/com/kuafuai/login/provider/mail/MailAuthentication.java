package com.kuafuai.login.provider.mail;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class MailAuthentication extends AbstractAuthenticationToken {

    private final Object principal;

    public MailAuthentication(Object principal) {
        super(null);
        this.principal = principal;
        super.setAuthenticated(false);
    }

    public MailAuthentication(Object principal, Collection<? extends GrantedAuthority> authorities) {
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
