package com.kuafuai.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "deploy")
@Data
public class DeployConfig {
    /**
     * 部署工作目录
     */
    private String workspaceDir;

    /**
     * 部署访问的基础URL
     */
    private String baseUrl;

    /**
     * zip文件最大大小（单位：MB）
     */
    private long maxFileSize = 100;
}
