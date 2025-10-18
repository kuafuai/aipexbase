package com.kuafuai.factory.sms.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsConfigRequest {
    private String configId;
    private String accessKeyId;
    private String accessKeySecret;
    private String signature;
    private String phone;
    private String templateId;
    private String templateName;
    private String code;
}