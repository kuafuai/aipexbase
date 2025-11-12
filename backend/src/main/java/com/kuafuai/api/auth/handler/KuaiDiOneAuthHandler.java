package com.kuafuai.api.auth.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuaidi100.sdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.Map;

@Component
@Slf4j
public class KuaiDiOneAuthHandler implements AuthHandler {

    private final Type configType = new TypeToken<Map<String, Object>>() {
    }.getType();
    private final Gson gson = new Gson();

    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) {
        if (apiMarket != null) {
            String authConfig = apiMarket.getAuthConfig();
            Map<String, Object> config = gson.fromJson(authConfig, configType);

            String key = (String) config.getOrDefault("api_key", "");
            String customer = (String) config.getOrDefault("app_id", "");

            String paramsStr = gson.toJson(params);
            String sign = SignUtils.querySign(paramsStr, key, customer);
            params.put("sign", sign);
            params.put("customer", customer);
            params.put("param", paramsStr);
        }
    }

    @Override
    public String getAuthType() {
        return "kuaidi100";
    }
}
