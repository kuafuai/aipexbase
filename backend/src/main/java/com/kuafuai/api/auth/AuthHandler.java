package com.kuafuai.api.auth;

import com.kuafuai.api.spec.ApiDefinition;
import com.kuafuai.system.entity.ApiMarket;
import com.kuafuai.system.entity.DynamicApiSetting;

import java.util.Map;

/**
 * 认证处理器接口
 */
public interface AuthHandler {

    /**
     * 处理认证逻辑
     *
     * @param apiDefinition API定义
     * @param params        请求参数
     */
    void handle(DynamicApiSetting setting, ApiDefinition apiDefinition, ApiMarket apiMarket, Map<String, Object> params);

    /**
     * 获取支持的认证类型
     *
     * @return 认证类型标识
     */
    String getAuthType();
}
