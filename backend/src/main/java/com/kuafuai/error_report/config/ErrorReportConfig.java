package com.kuafuai.error_report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "error-report")
@Data
public class ErrorReportConfig {
    private String reportUrl;

    private String secretKey;

}
