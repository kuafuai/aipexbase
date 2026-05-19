package com.kuafuai.login.provider.huawei;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.LoginHuaweiBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HwProvider implements AuthenticationProvider {

    @Autowired
    private LoginHuaweiBusinessService loginHuaweiBusinessService;

    @Autowired
    private LoginBusinessService loginBusinessService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        LoginVo loginVo = (LoginVo) authentication.getPrincipal();
        log.info("[HW Login] 开始华为登录, code={}", loginVo.getCode() != null ? "***" : "null");

        // 1. 授权码换 openID
        String openId = loginHuaweiBusinessService.getOpenId(loginVo.getCode());

        // 2. 查找已有用户，不存在则自动创建
        Login current = loginBusinessService.getUserByOpenId(openId, loginVo.getRelevanceTable());
        if (current == null) {
            current = loginBusinessService.createNewLoginByWechat(openId);
        }

        LoginUser loginUser = loginBusinessService.getLoginUser(current, loginVo.getRelevanceTable());
        return new HwAuthentication(loginUser, authentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(HwAuthentication.class);
    }
}
