package com.kuafuai.login.controller;


import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.provider.phone.PhoneAuthentication;
import com.kuafuai.login.service.LoginPhoneBusinessService;
import com.kuafuai.login.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LoginPhoneController {

    @Autowired
    private LoginPhoneBusinessService loginPhoneBusinessService;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @PostMapping("/login/phone/sendCode")
    public BaseResponse sendPhoneCode(@RequestBody LoginVo loginVo) {
        String appId = GlobalAppIdFilter.getAppId();
        String code = loginPhoneBusinessService.sendLoginCode(appId, loginVo.getPhone());
        return ResultUtils.success(code);
    }

    @PostMapping("/login/phone")
    public BaseResponse loginByPhoneCode(@RequestBody LoginVo loginVo) {
        PhoneAuthentication authentication = new PhoneAuthentication(loginVo);
        Authentication returnAuth = authenticationManager.authenticate(authentication);
        LoginUser loginUser = (LoginUser) returnAuth.getPrincipal();
        String token = tokenService.createToken(loginUser);

        return ResultUtils.success(token);
    }
}
