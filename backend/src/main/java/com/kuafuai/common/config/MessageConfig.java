package com.kuafuai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "message")
@Data
public class MessageConfig {
    private boolean enable;
    private String notifyUrl;
}
