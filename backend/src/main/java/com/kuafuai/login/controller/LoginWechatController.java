package com.kuafuai.login.controller;


import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.provider.wechat.WxWebAuthentication;
import com.kuafuai.login.service.LoginWechatBusinessService;
import com.kuafuai.login.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LoginWechatController {

    @Autowired
    private LoginWechatBusinessService loginWechatBusinessService;

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @GetMapping("/login/wechat")
    public BaseResponse getWechatLoginUrl() {
        String appId = GlobalAppIdFilter.getAppId();
        return ResultUtils.success(loginWechatBusinessService.getWechatAuthorize(appId));
    }

    @PostMapping("/login/wechat")
    public BaseResponse loginByWechat(@RequestBody LoginVo loginVo) {
        WxWebAuthentication authentication = new WxWebAuthentication(loginVo);
        Authentication returnAuth = authenticationManager.authenticate(authentication);
        LoginUser loginUser = (LoginUser) returnAuth.getPrincipal();
        String token = tokenService.createToken(loginUser);

        return ResultUtils.success(token);
    }
}
