package com.kuafuai.login.config;


import com.google.common.collect.Maps;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.util.SpringUtils;
import lombok.Data;

import java.util.Map;

@Data
public class WechatConfig {

    private String appId;

    private String appSecret;

    private String isRegister;

    private String mpAppId;

    private String mpRedirectUri;

    private String mpAppSecret;

    private String mpFrontendRedirectUri;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, String> parseMap = Maps.newHashMap();

        public Builder map(Map<String, String> map) {
            if (map != null && !map.isEmpty()) {
                map.forEach(parseMap::putIfAbsent);
            }
            return this;
        }

        public Builder appId(String appId) {
            DynamicConfigBusinessService dynamicConfigBusinessService = SpringUtils.getBean(DynamicConfigBusinessService.class);
            Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
            map.forEach(parseMap::putIfAbsent);
            return this;
        }

        public WechatConfig build() {
            WechatConfig config = new WechatConfig();

            config.appId = parseMap.getOrDefault("wechat.app-id", "xxxx");
            config.appSecret = parseMap.getOrDefault("wechat.app-secret", "xxxx");
            config.isRegister = parseMap.getOrDefault("wechat.is-register", "0");
            config.mpAppId = parseMap.getOrDefault("wx.pay.mp-app-id", "xxxxx");
            config.mpAppSecret = parseMap.getOrDefault("wx.pay.mp-app-secret", "xxx");
            config.mpRedirectUri = parseMap.getOrDefault("wechat.mp-redirect_uri", "xxxxx");
            config.mpFrontendRedirectUri = parseMap.getOrDefault("wechat.mp-frontend_redirect_uri", "xxxx");
            
            return config;
        }
    }
}


