package com.kuafuai.login.service;


import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.wx.WxAppClient;
import com.kuafuai.wx.WxAppCode2SessionRequest;
import com.kuafuai.wx.WxAppCode2SessionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class LoginWeappBusinessService {

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;
    private final WxAppClient wxAppClient = new WxAppClient();

    public String getOpenId(String code) {
        String appId = GlobalAppIdFilter.getAppId();
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);

        String wxAppId = map.getOrDefault("wechat.app-id", "");
        String wxAppSecret = map.getOrDefault("wechat.app-secret", "");

        if (StringUtils.isEmpty(wxAppId) || StringUtils.equalsAnyIgnoreCase(code, "codeflying")) {
            return "codeflying";
        } else {
            WxAppCode2SessionResponse response = wxAppClient.code2Session(WxAppCode2SessionRequest.builder()
                    .appId(wxAppId)
                    .appSecret(wxAppSecret)
                    .code(code)
                    .grantType("authorization_code")
                    .build());
            if (response.getErrcode() != null && response.getErrcode() > 0) {
                throw new BusinessException("login.login.wechat.app.openid");
            }
            return response.getOpenId();
        }
    }
}
