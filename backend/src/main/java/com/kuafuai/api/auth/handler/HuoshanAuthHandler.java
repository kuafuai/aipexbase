package com.kuafuai.api.auth.handler;


import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.auth.entity.HuoshanSignResult;
import com.kuafuai.api.auth.sign.HuoshanMusicSign;
import com.kuafuai.api.auth.sign.HuoshanSign;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HuoshanAuthHandler implements AuthHandler {

    private final Type configType = new TypeToken<Map<String, Object>>() {

    }.getType();
    private final Gson gson = new Gson();


    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) throws Exception {
        String authConfig = apiMarket.getAuthConfig();
        params.remove("ip");

        Map<String, Object> config = gson.fromJson(authConfig, configType);
        Map<String, String> queryMap = new HashMap<>();
        String region = (String) config.getOrDefault("region", "");
        String accessKey = (String) config.getOrDefault("accessKey", "");
        String secretKey = (String) config.getOrDefault("secretKey", "");
        String action = (String) config.getOrDefault("action", "");
        String version = (String) config.getOrDefault("version", "");
        String host = (String) config.getOrDefault("host", "");
        String service = (String) config.getOrDefault("service", "");

        if (StringUtils.equalsIgnoreCase(action, "CVSync2AsyncGetResult")) {
            Map<String, Object> req = Maps.newHashMap();
            req.put("return_url", true);
            params.put("req_json", gson.toJson(req));
        }

        String body = gson.toJson(params);

        HuoshanSignResult huoshanSignResult = HuoshanSign.signResult(accessKey, secretKey,
                apiDefinition.getMethod(), queryMap,
                body.getBytes(StandardCharsets.UTF_8), new Date(), action,
                version, host, service, region);

        if (StringUtils.equalsIgnoreCase(action, "CVSync2AsyncGetResult")) {
            // 重置参数
            Map<String, Object> req = Maps.newHashMap();
            req.put("return_url", true);

            String reqStr = gson.toJson(gson.toJson(req));
            reqStr = reqStr.substring(1, reqStr.length() - 1);
            params.put("req_json", reqStr);
        }


        params.put("host", host);
        params.put("xDate", huoshanSignResult.getXDate());
        params.put("xContentSha256", huoshanSignResult.getXContentSha256());
        params.put("authorization", huoshanSignResult.getAuthorization());

    }

    @Override
    public String getAuthType() {
        return "huoshan";
    }
}
