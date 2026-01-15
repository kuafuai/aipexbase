package com.kuafuai.api.auth.handler;

import com.kuafuai.api.auth.AuthHandler;
import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@Slf4j
public class XunfeiAuthHandler implements AuthHandler {

    @Override
    public void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params) {

    }

    @Override
    public String getAuthType() {
        return "xunfei";
    }
}
