package com.kuafuai.login.provider.passwd;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.base.CaseFormat;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Component

@Slf4j
public class UserNameProvider implements AuthenticationProvider {

    @Autowired
    private LoginBusinessService loginBusinessService;

    @Autowired
    private LoginService loginService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        LoginVo loginVo = (LoginVo) authentication.getPrincipal();
        String passwd = authentication.getCredentials().toString();

        Login current = loginBusinessService.getUserBySelectKey(loginVo.getPhone(), loginVo.getRelevanceTable());
        if (current == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        String dbPassword = current.getPassword();
        if (StringUtils.isEmpty(dbPassword)) {
            throw new BadCredentialsException("密码有误");
        }

        if (!SecurityUtils.matchesPassword(passwd, dbPassword)) {
            throw new BadCredentialsException("密码有误");
        }

        LoginUser loginUser = loginBusinessService.getLoginUser(current, loginVo.getRelevanceTable());
        final String openId = loginVo.getOpenId();
        //  如果登陆的时候携带了openId，那么保存数据到库中
        saveOpenId(loginUser, openId);

        return new UsernamePasswordAuthenticationToken(loginUser, "", authentication.getAuthorities());
    }


    /**
     * 如果登陆的时候携带了openId，那么保存数据到库中
     *
     * @param loginUser
     * @param openId
     */
    private void saveOpenId(LoginUser loginUser, String openId) {
        if (StringUtils.isEmpty(openId)) {
            return;
        }
        final String relevanceTable = loginUser.getRelevanceTable();
        final String relevanceId = loginUser.getRelevanceId();
        final String appId = loginUser.getAppId();


        final LambdaUpdateWrapper<Login> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Login::getWxOpenId, openId)
                .eq(Login::getRelevanceId, relevanceId)
                .eq(Login::getRelevanceTable, CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, relevanceTable));
        loginService.update(appId, updateWrapper);
    }




    @Override
    public boolean supports(Class<?> aClass) {
        return aClass.equals(UsernamePasswordAuthenticationToken.class);
    }
}
