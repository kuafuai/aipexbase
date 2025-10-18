package com.kuafuai.factory.sms.impl;

import com.kuafuai.factory.sms.SmsSendMessage;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.provider.config.BaseConfig;

public class AlibabaSendMessage implements SmsSendMessage {
    private AlibabaConfig alibabaConfig;

    @Override
    public BaseConfig buildSmsConfig(SmsConfigRequest smsConfigRequest) {
        alibabaConfig = new AlibabaConfig();
        alibabaConfig.setConfigId(smsConfigRequest.getConfigId());
        alibabaConfig.setAccessKeyId(smsConfigRequest.getAccessKeyId());
        alibabaConfig.setAccessKeySecret(smsConfigRequest.getAccessKeySecret());
        alibabaConfig.setSignature(smsConfigRequest.getSignature());
        alibabaConfig.setTemplateId(smsConfigRequest.getTemplateId());
        alibabaConfig.setTemplateName(smsConfigRequest.getTemplateName());
        return alibabaConfig;
    }

    public AlibabaConfig getAlibabaConfig() {
        return alibabaConfig;
    }
}
