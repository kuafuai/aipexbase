package com.kuafuai.api.auth.entity;

import lombok.Data;

@Data
public class HuoshanConfig {
    private String accessKey;
    private String secretKey;
    private String action;
    private String version;
    private String host;
    private String service;
    private String region;
}
