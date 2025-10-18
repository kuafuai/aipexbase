package com.kuafuai.login.controller;


import cn.hutool.http.HttpUtil;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.dynamic.service.DynamicService;
import com.kuafuai.login.config.WechatConfig;
import com.kuafuai.login.entity.LoginVo;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.service.LoginBusinessService;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.login.service.WxAppService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private TokenService tokenService;

    @Autowired
    private LoginBusinessService loginBusinessService;


    @Autowired
    private WxAppService wxAppService;
    @Autowired
    private DynamicService dynamicService;

    /**
     * 密码登录
     *
     * @param loginVo
     * @return
     */
    @PostMapping("/login/passwd")
    public BaseResponse loginByPasswd(@RequestBody LoginVo loginVo) {

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginVo, loginVo.getPassword());
        Authentication returnAuth = authenticationManager.authenticate(authenticationToken);
        LoginUser loginUser = (LoginUser) returnAuth.getPrincipal();
        String token = tokenService.createToken(loginUser);

        return ResultUtils.success(token);
    }

    @PostMapping("/login/register")
    public Object register(
            @RequestParam(value = "table", required = false) String table,
            @RequestBody Map<String, Object> data) {
        try {
            String appId = GlobalAppIdFilter.getAppId();
            if (StringUtils.isEmpty(table)) {
                table = DynamicAuthFilter.getAppInfo().getAuthTable();
            }

            BaseResponse response = dynamicService.add(appId, table, data);
            Object id = response.getData();
            if (id != null) {
                return ResultUtils.success(loginBusinessService.getUserById(id, appId, table));
            } else {
                return response;
            }
        } catch (Exception e) {
            return ResultUtils.error(e.getMessage());
        }
    }


    @GetMapping("/getUserInfo")
    public BaseResponse getCurrentUser() {
        final Object currentUser = loginBusinessService.getCurrentUser();
        if (currentUser instanceof Boolean) {
            return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        } else {
            return ResultUtils.success(currentUser);
        }
    }


    @GetMapping("/login/openid")
    @ApiOperation("获取小程序openid")
    public BaseResponse getOpenId(@Validated @RequestParam @NotBlank(message = "code不能为空") String code) {
        //1.根据code换取openid
        try {
            String openid = wxAppService.getOpenId(code);
            return ResultUtils.success(openid);
        } catch (Exception e) {

            return ResultUtils.success(null, e.getMessage());
        }
    }


    @PostMapping("get_mp_url")
    public BaseResponse<String> getMpUrl() {
        try {
            WechatConfig wechatConfig = WechatConfig.builder()
                    .appId(GlobalAppIdFilter.getAppId())
                    .build();

            final String mpAppId = wechatConfig.getMpAppId();
            final String mpRedirectUri = wechatConfig.getMpRedirectUri();

            if (StringUtils.isEmpty(mpAppId) || !mpAppId.startsWith("wx")) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "mp_appid is not null");
            }

            if (StringUtils.isEmpty(mpRedirectUri) || !mpRedirectUri.startsWith("http")) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "mpRedirectUri is not null");
            }
            String urlTemplate = "https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri=" +
                    "{}&response_type=code&scope=snsapi_base&state=123421#wechat_redirect";


            final String format = StringUtils.format(urlTemplate, wechatConfig.getMpAppId(), wechatConfig.getMpRedirectUri());
            log.info("【获取公众号授权链接】,urlTemplate:{}", format);
            return ResultUtils.success(format);
        } catch (Exception e) {
            return ResultUtils.success(null, e.getMessage());
        }

    }

    @GetMapping("login/redirect/{database}")
    public RedirectView redirect(@PathVariable String database, @RequestParam String code) {
        try {
            log.info("【微信登录】,code:{}", code);

            WechatConfig wechatConfig = WechatConfig.builder()
                    .appId(database)
                    .build();

            final String mpAppId = wechatConfig.getMpAppId();
            final String mpAppSecret = wechatConfig.getMpAppSecret();

            String urlTemplate = "https://api.weixin.qq.com/sns/oauth2/access_token?appid={}&secret={}&code={}&grant_type=authorization_code";

            final String format = StringUtils.format(urlTemplate, mpAppId, mpAppSecret, code);
            final String string = HttpUtil.post(format, new HashMap<>());
            final Map map = JSON.parseObject(string, Map.class);
            String openid = null;
            if (map.containsKey("openid")) {
                openid = String.valueOf(map.get("openid"));
            } else {
                log.error("【获取openid失败】,code:{},string:{}", code, string);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, String.valueOf(map.get("errmsg")));
            }
            final String mpFrontendRedirectUri = wechatConfig.getMpFrontendRedirectUri();
            String redirectUrl;
            if (!mpFrontendRedirectUri.contains("?")) {
                redirectUrl = wechatConfig.getMpFrontendRedirectUri() + "?openid=" + openid;
            } else {
                redirectUrl = wechatConfig.getMpFrontendRedirectUri() + "&openid=" + openid;
            }
            log.info("【微信登录】,code:{},openid:{},redictUrl:{}", code, openid, redirectUrl);

            return new RedirectView(redirectUrl);
        } finally {
            ConfigContext.clear();
        }


    }

}
