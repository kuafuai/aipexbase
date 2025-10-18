package com.kuafuai.login.provider.wechat;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.LoginWechatBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WxWebProvider implements AuthenticationProvider {

    @Autowired
    private LoginWechatBusinessService loginWechatBusinessService;

    @Autowired
    private LoginBusinessService loginBusinessService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        LoginVo loginVo = (LoginVo) authentication.getPrincipal();
        log.info("{}", loginVo);
        //1.根据code换取openid
        String openid = loginWechatBusinessService.getOpenId(loginVo.getCode());

        Login current = loginBusinessService.getUserByOpenId(openid, loginVo.getRelevanceTable());
        if (current == null) {
            // 创建一个用户
            current = loginBusinessService.createNewLoginByWechat(openid);
        }
        LoginUser loginUser = loginBusinessService.getLoginUser(current, loginVo.getRelevanceTable());
        return new WxWebAuthentication(loginUser, authentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {

        return authentication.equals(WxWebAuthentication.class);
    }
}
