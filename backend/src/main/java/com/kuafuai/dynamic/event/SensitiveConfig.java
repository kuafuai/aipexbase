package com.kuafuai.dynamic.event;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sensitive")
@Data
public class SensitiveConfig {
    private boolean enable;

    private String appKey;
    private String secretKey;
}
