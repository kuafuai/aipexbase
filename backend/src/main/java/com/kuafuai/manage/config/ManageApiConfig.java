package com.kuafuai.manage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 管理API配置类
 * 控制对外开放的管理接口（创建应用、创建表等）
 */
@Configuration
@ConfigurationProperties(prefix = "manage.api")
@Data
public class ManageApiConfig {
    /**
     * 是否启用对外API
     */
    private boolean enable = false;

    /**
     * API访问令牌
     */
    private String token;
}
