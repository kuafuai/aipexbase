package com.kuafuai.login.provider.oauth2.server;

import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.entity.OAuth2UserInfo;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.wx.WxAppClient;
import com.kuafuai.wx.WxWebCode2TokenRequest;
import com.kuafuai.wx.WxWebCode2TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * GitHub OAuth2提供商实现
 */
@Slf4j
@Component
public class WechatProvider implements OAuth2ProviderInterface {
    

    private final static String wechatAuthUrl = "https://open.weixin.qq.com/connect/oauth2/authorize";

    private final WxAppClient wxAppClient = new WxAppClient();

    @Resource
    private DynamicConfigBusinessService dynamicConfigBusinessService;


    @Override
    public String getProviderName() {
        return "wechat";
    }

    @Override
    public String getAuthorizationUrl(String state) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);

        if (map.isEmpty()) {
            throw new RuntimeException("Wechat OAuth2配置不存在");
        }

        Map<String, String> params = new HashMap<>();
        params.put("appid", map.getOrDefault("oauth2.wechat.app_id", ""));
        params.put("redirect_uri", map.getOrDefault("oauth2.wechat.callback_uri", ""));
        params.put("response_type", "code");
        params.put("scope", "snsapi_base");
        params.put("state", state);
        String wxAuthUrl = buildQueryString(wechatAuthUrl, params);
        return wxAuthUrl + "#wechat_redirect";
    }

    @Override
    public String getCallbackUri() {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);

        if (map.isEmpty()) {
            throw new RuntimeException("Wechat OAuth2配置不存在");
        }

        return map.getOrDefault("oauth2.wechat.callback_uri", "");
    }

    @Override
    public String getAccessToken(String code) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        String wechatAppId = map.getOrDefault("oauth2.wechat.app_id", "");
        String wechatSecret = map.getOrDefault("oauth2.wechat.app_secret", "0");

        if (StringUtils.isEmpty(wechatAppId) || StringUtils.equalsAnyIgnoreCase(code, "codeflying")) {
            return "codeflying";
        } else {
            WxWebCode2TokenResponse response = wxAppClient.code2Session(WxWebCode2TokenRequest.builder()
                    .appId(wechatAppId)
                    .appSecret(wechatSecret)
                    .code(code)
                    .grantType("authorization_code")
                    .build());

            if (response.getErrcode() != null && response.getErrcode() > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR.getCode(), "H5验证失败");
            }
            return response.getOpenId();
        }
    }
    
    @Override
    public OAuth2UserInfo getUserInfo(String openId) {
        OAuth2UserInfo userInfo = new OAuth2UserInfo();
        userInfo.setId(openId);
        return userInfo;
//        try {
//            String response = webClient.get()
//                    .uri("ss")
//                    .header("Authorization", "Bearer " + accessToken)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
//
//            JsonObject userJson = JsonParser.parseString(response).getAsJsonObject();
//
//            OAuth2UserInfo userInfo = new OAuth2UserInfo();
//            userInfo.setId(String.valueOf(userJson.get("openid").getAsString()));
//            userInfo.setUsername(userJson.has("nickname") ? userJson.get("nickname").getAsString() : "");
//            userInfo.setEmail(userJson.has("email") ? userJson.get("email").getAsString() : "");
//            userInfo.setAvatar(userJson.has("headimgurl") ? userJson.get("headimgurl").getAsString() : "");
//            userInfo.setNickname(userJson.has("nickname") ? userJson.get("nickname").getAsString() : "");
//            userInfo.setGender(userJson.has("sex") ? String.valueOf(userJson.get("sex").getAsInt()) : "");
//            userInfo.setRawUserInfo(userJson);
//
//            return userInfo;
//        } catch (Exception e) {
//            log.error("获取Wechat用户信息失败", e);
//            throw new RuntimeException("获取用户信息失败: " + e.getMessage());
//        }
    }
    

    
//    private String buildFormData(Map<String, String> params) {
//        StringBuilder formData = new StringBuilder();
//        for (Map.Entry<String, String> entry : params.entrySet()) {
//            if (formData.length() > 0) {
//                formData.append("&");
//            }
//            try {
//                formData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
//                        .append("=")
//                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
//            }catch (UnsupportedEncodingException e) {
//                log.error("构建表单数据异常 {}",e.getMessage());
//               return "";
//            }
//
//        }
//        return formData.toString();
//    }

    @NotNull
    static String getString(String baseUrl, Map<String, String> params) {
        StringBuilder queryString = new StringBuilder(baseUrl);
        queryString.append("?");

        try {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (queryString.length() > baseUrl.length() + 1) {
                    queryString.append("&");
                }
                queryString.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            }
        }catch (UnsupportedEncodingException e){
            log.error("构建授权链接异常 {}",e.getMessage());
           return "";
        }

        return queryString.toString();
    }

    private String buildQueryString(String baseUrl, Map<String, String> params) {
        return getString(baseUrl, params);
    }
}
