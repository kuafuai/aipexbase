package com.kuafuai.factory.sms.impl;

import com.kuafuai.factory.sms.SmsSendMessage;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import org.dromara.sms4j.provider.config.BaseConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;

public class TencentSendMessage implements SmsSendMessage {
    private TencentConfig tencentConfig;

    @Override
    public BaseConfig buildSmsConfig(SmsConfigRequest smsConfigRequest) {
        tencentConfig = new TencentConfig();
        tencentConfig.setConfigId(smsConfigRequest.getConfigId());
        tencentConfig.setAccessKeyId(smsConfigRequest.getAccessKeyId());
        tencentConfig.setAccessKeySecret(smsConfigRequest.getAccessKeySecret());
        tencentConfig.setSignature(smsConfigRequest.getSignature());
        tencentConfig.setTemplateId(smsConfigRequest.getTemplateId());
        return tencentConfig;
    }

    public TencentConfig getTencentConfig() {
        return tencentConfig;
    }
}
