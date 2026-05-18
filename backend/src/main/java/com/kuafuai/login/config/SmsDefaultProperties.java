package com.kuafuai.login.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "sms.default")
public class SmsDefaultProperties {

    private boolean enabled;
    private String supplier;
    private String appId;
    private String appSecret;
    private String signName;
    private String templateCode;
    private String templateParam;
    private BigDecimal unitPrice;
    private RateLimit rateLimit = new RateLimit();

    public boolean isUsable() {
        return enabled
                && StringUtils.isNotBlank(appId)
                && StringUtils.isNotBlank(appSecret)
                && StringUtils.isNotBlank(supplier)
                && StringUtils.isNotBlank(templateCode);
    }

    @Data
    public static class RateLimit {
        private int perMinute = 1;
        private int perHour = 10;
    }
}
