package com.kuafuai.login.service;

import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.wx.WxAppClient;
import com.kuafuai.wx.WxWebCode2TokenRequest;
import com.kuafuai.wx.WxWebCode2TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.util.Map;

@Service
public class LoginWechatBusinessService {

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;

    private final WxAppClient wxAppClient = new WxAppClient();

    public String getWechatAuthorize(String appId) {
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        String wechatAppId = map.getOrDefault("wx.pay.mp-app-id", "");

        String redirectUri = URLEncoder.encode(StringUtils.format("https://view.kuafuai.net/1{}/", appId));
        return StringUtils.format("https://open.weixin.qq.com/connect/oauth2/authorize?appid={}&redirect_uri={}&response_type=code&scope=snsapi_base&state=123421#wechat_redirect",
                wechatAppId, redirectUri);
    }

    public String getOpenId(String code) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        String wechatAppId = map.getOrDefault("wx.pay.mp-app-id", "");
        String wechatSecret = map.getOrDefault("wx.pay.mp-app-secret", "0");

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
                throw new BusinessException("login.login.wechat.h5.openid");
            }
            return response.getOpenId();
        }
    }
}
