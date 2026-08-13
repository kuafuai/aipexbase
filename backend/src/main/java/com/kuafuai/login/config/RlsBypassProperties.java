package com.kuafuai.login.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "security.bypass-rls")
public class RlsBypassProperties {

    private boolean enabled = true;

    private String header = "X-Bypass-Rls";

    private String seed = "kuafu-rls-bypass-default-seed";

    private int windowSeconds = 30;

    private int skew = 1;
}
