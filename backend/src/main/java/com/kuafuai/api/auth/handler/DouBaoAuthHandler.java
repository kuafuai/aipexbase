package com.kuafuai.api.auth.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

@Component
@Slf4j
public class DouBaoAuthHandler implements AuthHandler {

    private final Type configType = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final Gson gson = new Gson();

    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) {
        if (apiMarket != null) {
            String authConfig = apiMarket.getAuthConfig();
            Map<String, Object> config = gson.fromJson(authConfig, configType);

            String appId = (String) config.getOrDefault("app_id", "");
            params.put("appId", appId);
            params.put("token", apiMarket.getToken());
        }
    }

    @Override
    public String getAuthType() {
        return "doubao";
    }
}
