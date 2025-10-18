package com.kuafuai.manage.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.manage.entity.vo.ManageLoginVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.manage.service.ManageUserNameAuthentication;
import com.kuafuai.system.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class ManageLoginController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private ManageBusinessService manageBusinessService;

    @PostMapping("/login")
    public BaseResponse login(@RequestBody ManageLoginVo loginVo) {
        ManageUserNameAuthentication authentication = new ManageUserNameAuthentication(loginVo, loginVo.getPassword());
        Authentication returnAuth = authenticationManager.authenticate(authentication);
        LoginUser loginUser = (LoginUser) returnAuth.getPrincipal();
        String token = tokenService.createToken(loginUser);

        return ResultUtils.success(token);
    }

    @PostMapping("/register")
    public BaseResponse register(@RequestBody Users users) {
        if (StringUtils.isEmpty(users.getEmail())) {
            throw new BusinessException("参数有误");
        }
        if (StringUtils.isEmpty(users.getPassword())) {
            throw new BusinessException("参数有误");
        }

        return manageBusinessService.register(users) ? ResultUtils.success() : ResultUtils.error("邮箱已被使用");
    }
}
