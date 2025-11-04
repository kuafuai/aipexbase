package com.kuafuai.login.controller;

import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.entity.OAuth2LoginRequest;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.provider.oauth2.OAuth2Authentication;
import com.kuafuai.login.service.OAuth2Service;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * OAuth2登录控制器
 * 提供OAuth2第三方登录相关的REST API接口
 */
@RestController
@RequestMapping("/oauth2")
@Slf4j
public class OAuth2LoginController {

    @Resource
    private Cache cache;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private TokenService tokenService;

    @Resource
    private OAuth2Service oauth2Service;

    @Resource
    private AppInfoService appInfoService;

    /**
     * 获取指定提供商的OAuth2授权URL
     * @param provider OAuth2提供商名称 / wechat / google
     * @return 包含授权URL和state的响应
     */
    @GetMapping("/authorize/{provider}")
    public BaseResponse getAuthorizationUrl(@PathVariable String provider) {

        try {
            String appId = GlobalAppIdFilter.getAppId();

            // 将 state 存储到缓存中，设置 10 分钟过期时间
            String state = UUID.randomUUID().toString().replace("-", "");

            // 设置缓存，同时存储重定向URL
            String stateKey = "oauth2_state:" + state;

            cache.setCacheObject(stateKey, appId, 10, TimeUnit.MINUTES);

            // 获取授权链接
            String authorizationUrl = oauth2Service.getAuthorizationUrl(provider, state);

            return ResultUtils.success(authorizationUrl);
        } catch (Exception e) {
            log.error("获取OAuth2授权URL失败", e);
            return ResultUtils.error("获取授权URL失败: " + e.getMessage());
        }
    }

    /**
     * 处理OAuth2登录回调
     * @param provider OAuth2提供商名称
     * @param code 授权码
     * @param state 状态参数
     * @return 重定向到指定URL
     */
    @GetMapping("/callback/{provider}")
    public ResponseEntity<String> oauth2Callback(@PathVariable("provider") String provider, @RequestParam String code, @RequestParam String state) {
        try {
            if (StringUtils.isAnyEmpty(provider, code, state)) {
                log.error("OAuth2回调参数缺失: provider={}, code={}, state={}", provider, code, state);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/error?message=oauth2_callback_failed"))
                        .build();
            }

            // 验证 state 参数，并获取 appid
            String appId = validateState(state);
            if (StringUtils.isEmpty(appId)) {
                log.error("OAuth2 CallBack state 验证失败: provider={}, state={}, appId={}", provider, state, appId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/error?message=invalid_state"))
                        .build();
            }

            AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
            if (Objects.isNull(appInfo)) {
                log.error("OAuth2 CallBack App 验证失败: provider={}, state={}, appId={}", provider, state, appId);
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/error?message=invalid_param"))
                        .build();
            }

            // 设置全局 id 后面db 会用
            GlobalAppIdFilter.setAppId(appId);

            // 创建OAuth2LoginRequest对象
            OAuth2LoginRequest loginRequest = new OAuth2LoginRequest();
            loginRequest.setProvider(provider);
            loginRequest.setCode(code);
            loginRequest.setState(state);
            loginRequest.setRelevanceTable(appInfo.getAuthTable());

            // 处理登录逻辑
            String token = processOAuth2Login(loginRequest);
            
            if (!StringUtils.isEmpty(token)) {
                String callbackUri = oauth2Service.getCallbackUri(provider);

                // 构建重定向URL，将token作为参数传递
                String callbackUrlWithToken = callbackUri + "?token=" + token + "&login_success=true";
                log.info("OAuth2登录成功，重定向到: {}", callbackUrlWithToken);
                
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(callbackUrlWithToken))
                        .build();
            } else {
                // todo 跳转统一的错误页
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create("/error?message=login_failed&error=generate_token_failed"))
                        .build();
            }

        } catch (Exception e) {
            log.error("OAuth2回调处理失败", e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/error?message=callback_error&error=" + e.getMessage()))
                    .build();
        }
    }

    /**
     * 处理OAuth2登录的核心逻辑
     * @param request OAuth2登录请求
     * @return 登录结果 Token
     */
    private String processOAuth2Login(OAuth2LoginRequest request) {
        try {
            // 创建认证令牌
            OAuth2Authentication authenticationToken = new OAuth2Authentication(request);

            // 进行认证
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();

            // 生成JWT令牌
            String token = tokenService.createToken(loginUser);

            // 验证成功后删除state，防止重复使用
            removeState(request.getState());

            return token;
        } catch (Exception e) {
            log.error("OAuth 2登录失败", e);
            return "";
        }
    }


    /**
     * 验证state参数是否有效
     * @param state 状态参数
     * @return appId
     */
    private String validateState(String state) {
        if (!StringUtils.hasText(state)) {
            return "";
        }

        String stateKey = "oauth2_state:" + state;

        String appId = cache.getCacheObject(stateKey);

        if (StringUtils.isEmpty(appId)) {
            return "";
        }

        return appId;
    }

    /**
     * 删除已使用的state参数
     * @param state 状态参数
     */
    private void removeState(String state) {
        if (!StringUtils.hasText(state)) {
            return;
        }
        String stateKey = "oauth2_state:" + state;
        cache.deleteObject(stateKey);
    }
}
