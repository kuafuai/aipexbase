package com.kuafuai.login.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.entity.GoogleIdTokenLoginRequest;
import com.kuafuai.login.entity.OAuth2UserInfo;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.login.provider.oauth2.server.GoogleProvider;
import com.kuafuai.login.service.OAuth2Service;
import com.kuafuai.login.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Google 登录控制器
 * 处理前端通过 Google Identity Services 拿到的 ID Token，验签后签发自己的 token
 */
@RestController
@Slf4j
public class LoginGoogleController {

    @Autowired
    private GoogleProvider googleProvider;

    @Autowired
    private OAuth2Service oauth2Service;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login/google")
    public BaseResponse loginByGoogle(@RequestBody GoogleIdTokenLoginRequest req) {
        if (req == null || StringUtils.isEmpty(req.getIdToken())) {
            return ResultUtils.error("缺少 idToken");
        }

        try {
            OAuth2UserInfo userInfo = googleProvider.verifyIdToken(req.getIdToken());

            String relevanceTable = req.getRelevanceTable();
            if (StringUtils.isEmpty(relevanceTable)) {
                relevanceTable = DynamicAuthFilter.getAppInfo().getAuthTable();
            }

            LoginUser loginUser = oauth2Service.findOrCreateUser(userInfo, relevanceTable);
            String token = tokenService.createToken(loginUser);

            return ResultUtils.success(token);
        } catch (Exception e) {
            log.error("Google ID Token 登录失败", e);
            return ResultUtils.error("登录失败: " + e.getMessage());
        }
    }
}
