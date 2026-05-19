package com.kuafuai.login.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "huawei")
public class HuaweiConfig {

    private String clientId;
    private String clientSecret;
}

