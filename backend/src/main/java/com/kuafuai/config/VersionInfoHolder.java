package com.kuafuai.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class VersionInfoHolder {
    private String version;

    /**
     * 最新版本
     */
    private String latestVersion;

    /**
     * 是否有更新
     */
    private boolean hasUpdate;

    /**
     * 更新说明
     */
    private String notes;

    /**
     * 提示信息
     */
    private String message;
}
