package com.kuafuai.api.auth.handler;


import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.auth.entity.HuoshanConfig;
import com.kuafuai.api.auth.entity.HuoshanSignResult;
import com.kuafuai.api.auth.sign.HuoshanSign;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.common.text.Convert;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class HuoshanAuthHandler implements AuthHandler {

    private final Gson gson = new Gson();

    private final List<HuoshanParamProcessor> processors;

    public HuoshanAuthHandler(List<HuoshanParamProcessor> processors) {
        this.processors = processors;
    }


    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) throws Exception {
        String authConfig = apiMarket.getAuthConfig();
        params.remove("ip");

        HuoshanConfig config = gson.fromJson(authConfig, HuoshanConfig.class);
        String reqKey = Convert.toStr(params.get("req_key"));

        HuoshanParamProcessor processor =
                processors.stream()
                        .filter(p -> p.match(reqKey))
                        .findFirst()
                        .orElse(null);

        if (processor == null) {
            return;
        }

        processor.beforeSign(config, params);

        String body = gson.toJson(params);

        HuoshanSignResult huoshanSignResult = HuoshanSign.signResult(
                config.getAccessKey(),
                config.getSecretKey(),
                apiDefinition.getMethod(),
                Maps.newHashMap(),
                body.getBytes(StandardCharsets.UTF_8),
                new Date(),
                config.getAction(),
                config.getVersion(),
                config.getHost(), config.getService(),
                config.getRegion());


        processor.afterSign(config, params);

        params.put("host", config.getHost());
        params.put("xDate", huoshanSignResult.getXDate());
        params.put("xContentSha256", huoshanSignResult.getXContentSha256());
        params.put("authorization", huoshanSignResult.getAuthorization());

        apiDefinition.setBodyTemplate(processor.template(config));
    }

    @Override
    public String getAuthType() {
        return "huoshan";
    }
}
