package com.kuafuai.login.provider.phone;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.LoginPhoneBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PhoneProvider implements AuthenticationProvider {

    @Autowired
    private LoginPhoneBusinessService loginPhoneBusinessService;
    @Autowired
    private LoginBusinessService loginBusinessService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        LoginVo loginVo = (LoginVo) authentication.getPrincipal();
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();

        String cacheCode = loginPhoneBusinessService.getLoginCode(phone);
        if (StringUtils.isEmpty(cacheCode)) {
            throw new BusinessException("login.login.code.expired");
        }

        if (!StringUtils.endsWithIgnoreCase(cacheCode, code)) {
            throw new BusinessException("login.login.code.error");
        }

        Login current = loginBusinessService.getUserBySelectKey(phone, loginVo.getRelevanceTable());
        if (current == null) {
            throw new UsernameNotFoundException("");
        }

        LoginUser loginUser = loginBusinessService.getLoginUser(current, loginVo.getRelevanceTable());

        return new PhoneAuthentication(loginUser, authentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(PhoneAuthentication.class);
    }
}
