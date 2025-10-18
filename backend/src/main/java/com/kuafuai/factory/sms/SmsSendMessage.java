package com.kuafuai.factory.sms;

import com.kuafuai.factory.sms.request.SmsConfigRequest;
import org.dromara.sms4j.provider.config.BaseConfig;


public interface SmsSendMessage {
    public BaseConfig buildSmsConfig(SmsConfigRequest smsConfigRequest);
}
