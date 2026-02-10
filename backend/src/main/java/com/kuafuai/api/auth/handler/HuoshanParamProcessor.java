package com.kuafuai.api.auth.handler;

import com.kuafuai.api.auth.entity.HuoshanConfig;

import java.util.Map;

public interface HuoshanParamProcessor {

    boolean match(String reqKey);

    void beforeSign(HuoshanConfig config, Map<String, Object> params);

    void afterSign(HuoshanConfig config, Map<String, Object> params);

    String template(HuoshanConfig config);
}
