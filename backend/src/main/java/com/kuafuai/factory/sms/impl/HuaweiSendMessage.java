package com.kuafuai.factory.sms.impl;

import com.kuafuai.factory.sms.SmsSendMessage;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.provider.config.BaseConfig;

public class HuaweiSendMessage implements SmsSendMessage {
    private HuaweiConfig huaweiConfig;
    @Override
    public BaseConfig buildSmsConfig(SmsConfigRequest smsConfigRequest) {
        huaweiConfig = new HuaweiConfig();
        huaweiConfig.setConfigId(smsConfigRequest.getConfigId());
        huaweiConfig.setAccessKeyId(smsConfigRequest.getAccessKeyId());
        huaweiConfig.setAccessKeySecret(smsConfigRequest.getAccessKeySecret());
        huaweiConfig.setSignature(smsConfigRequest.getSignature());
        huaweiConfig.setTemplateId(smsConfigRequest.getTemplateId());
        return huaweiConfig;
    }

    public HuaweiConfig getHuaweiConfig() {
        return huaweiConfig;
    }
}
