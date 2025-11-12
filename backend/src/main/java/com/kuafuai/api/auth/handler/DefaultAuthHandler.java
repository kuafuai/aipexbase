package com.kuafuai.api.auth.handler;


import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class DefaultAuthHandler implements AuthHandler {
    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) {
        String token = apiMarket != null ? apiMarket.getToken() : setting.getToken();
        if (StringUtils.isNotEmpty(token)) {
            params.put("token", token);
        }
    }

    @Override
    public String getAuthType() {
        return "default";
    }
}
