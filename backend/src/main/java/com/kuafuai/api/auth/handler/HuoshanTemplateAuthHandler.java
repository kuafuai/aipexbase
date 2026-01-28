package com.kuafuai.api.auth.handler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.auth.entity.HuoshanSignResult;
import com.kuafuai.api.auth.sign.HuoshanTemplateSign;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class HuoshanTemplateAuthHandler implements AuthHandler {
    private final Type configType = new TypeToken<Map<String, Object>>() {

    }.getType();
    private final Gson gson = new Gson();


    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) throws Exception {
        if (apiMarket != null) {
            String authConfig = apiMarket.getAuthConfig();
            Map<String, Object> config = gson.fromJson(authConfig, configType);
            Map<String, String> queryMap = new HashMap<>();
            String accessKey = (String) config.getOrDefault("accessKey", "");
            String secretKey = (String) config.getOrDefault("secretKey", "");
            String action = (String) config.getOrDefault("action", "");
            String version = (String) config.getOrDefault("version", "");
            String service = (String) config.getOrDefault("service", "");
            String ip = params.get("ip").toString();
            params.remove("ip");
            String body = "";
            String payloadJson = "";
            if ("QueryAiTemplateTaskResult".equals( action)){
                body = gson.toJson(params);
            }else {
                HashMap<String, Object> bodyMap = new HashMap<>();
                HashMap<String, Object> subMap = new HashMap<>();
                subMap.put("ResourceList", params.get("resourceList"));
                subMap.put("TemplateId", params.get("templateId").toString());
                bodyMap.put("ServerId", params.get("serverId"));
                payloadJson =  gson.toJson(subMap);
                bodyMap.put("PayloadJson", payloadJson);
                body = gson.toJson(bodyMap);

            }
            HuoshanSignResult huoshanSignResult = HuoshanTemplateSign.signResult("POST", queryMap, body, action, version, service, accessKey, secretKey);
            String xDate = huoshanSignResult.getXDate();
            String xContentSha256 = huoshanSignResult.getXContentSha256();
            String authorization = huoshanSignResult.getAuthorization();

            String payloadJsonStr = gson.toJson(payloadJson);
            payloadJsonStr = payloadJsonStr.substring(1, payloadJsonStr.length() - 1);

            params.put("xDate", xDate);
            params.put("xContentSha256", xContentSha256);
            params.put("authorization", authorization);
            params.put("payloadJson", payloadJsonStr);
            params.put("ip", ip);

        }
    }

    @Override
    public String getAuthType() {
        return "huoshanTemplate";
    }
}
