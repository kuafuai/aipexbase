package com.kuafuai.login.service;

import cn.hutool.core.util.RandomUtil;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.factory.SmsConfigFactory;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.javase.config.SEInitializer;
import org.dromara.sms4j.provider.config.SmsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginPhoneBusinessService {

    @Resource
    private Cache cache;

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;


    private static final String CACHE_PREFIX = "login:phone:";
    private static final String CONFIG_SMS_APP_ID = "sms.app-id";
    private static final String CONFIG_SMS_SECRET = "sms.app-secret";
    private static final String CONFIG_SMS_SIGN_NAME = "sms.sign_name";
    private static final String CONFIG_SMS_CODE_TEMPLATE = "sms.code_template";
    private static final String CONFIG_SMS_PARAM_TEMPLATE = "sms.param_template";
    private static final String CONFIG_SMS_SUPPLIER = "sms.supplier";
    private static final int LOGIN_CODE_EXPIRE_MINUTES = 5;

    public String sendLoginCode(String appId, String phone) {
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        String numbers = RandomUtil.randomNumbers(6);

        //配置了host等参数，走发送流程
        if (map.containsKey(CONFIG_SMS_SUPPLIER)) {
            String accessKeyId = map.getOrDefault(CONFIG_SMS_APP_ID, "");
            String accessKeySecret = map.getOrDefault(CONFIG_SMS_SECRET, "");
            String signature = map.getOrDefault(CONFIG_SMS_SIGN_NAME, "北京跨赴科技");
            String templateId = map.getOrDefault(CONFIG_SMS_CODE_TEMPLATE, "SMS_487525448");
            String code = map.getOrDefault(CONFIG_SMS_PARAM_TEMPLATE, "code");
            String supplier = map.getOrDefault(CONFIG_SMS_SUPPLIER, "alibaba");
            SmsConfigRequest smsConfigRequest = SmsConfigRequest.builder()
                    .configId(supplier)
                    .accessKeyId(accessKeyId)
                    .accessKeySecret(accessKeySecret)
                    .signature(signature)
                    .templateId(templateId)
                    .templateName(code).build();
            // 短信配置
            SmsConfigFactory smsConfigFactory = new SmsConfigFactory();
            smsConfigFactory.getSmsConfig(supplier, smsConfigRequest);
            SEInitializer.initializer().fromConfig(new SmsConfig(), smsConfigFactory.getSupplierConfigList(supplier));
            SmsResponse smsResponse = SmsFactory.getSmsBlend(supplier).sendMessage(phone, numbers);
            if (ObjectUtils.isEmpty(smsResponse) || !smsResponse.isSuccess()) {
                throw new BusinessException("login.login.phone.sms");
            }
            cache.setCacheObject(buildCacheKey(phone), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return "";
        } else {
            //没有配置走mock流程
            cache.setCacheObject(buildCacheKey(phone), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return numbers;
        }
    }

    public String getLoginCode(String phone) {
        return cache.getCacheObject(buildCacheKey(phone));
    }

    private String buildCacheKey(String phone) {
        return CACHE_PREFIX + phone;
    }
}
