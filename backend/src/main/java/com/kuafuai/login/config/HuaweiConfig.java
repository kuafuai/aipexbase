package com.kuafuai.login.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "huawei")
public class HuaweiConfig {

    /** JWT Header kid（凭据文件 key_id） */
    private String keyId;

    /** RSA 私钥 PEM（PS256 签名用，凭据文件 private_key） */
    private String privateKey;
}
