package com.kuafuai.login.service;

import cn.hutool.core.util.RandomUtil;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.exception.RateLimitException;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.factory.SmsConfigFactory;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import com.kuafuai.login.config.SmsDefaultProperties;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.ApiBillingService;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.UserBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.dromara.sms4j.javase.config.SEInitializer;
import org.dromara.sms4j.provider.config.SmsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoginPhoneBusinessService {

    @Resource
    private Cache cache;

    @Autowired
    private DynamicConfigBusinessService dynamicConfigBusinessService;

    @Autowired
    private SmsDefaultProperties smsDefaults;

    @Autowired
    private AppInfoService appInfoService;

    @Autowired
    private UserBalanceService userBalanceService;

    @Autowired
    private ApiBillingService apiBillingService;

    @Autowired
    private SmsRateLimitService smsRateLimitService;


    private static final String CACHE_PREFIX = "login:phone:";
    private static final String CONFIG_SMS_APP_ID = "sms.app-id";
    private static final String CONFIG_SMS_SECRET = "sms.app-secret";
    private static final String CONFIG_SMS_SIGN_NAME = "sms.sign_name";
    private static final String CONFIG_SMS_CODE_TEMPLATE = "sms.code_template";
    private static final String CONFIG_SMS_PARAM_TEMPLATE = "sms.param_template";
    private static final String CONFIG_SMS_SUPPLIER = "sms.supplier";
    private static final String CONFIG_LOGIN_PHONE_MOCK = "login.show.phone";
    private static final int LOGIN_CODE_EXPIRE_MINUTES = 5;
    private static final int BILLING_MODEL_PER_USE = 1;

    @Transactional(rollbackFor = Exception.class)
    public String sendLoginCode(String appId, String phone) {
        Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
        String numbers = RandomUtil.randomNumbers(6);

        // per-app mock 开关：显式开启才走 mock，缺省/false 一律走付费路径
        if ("true".equalsIgnoreCase(map.getOrDefault(CONFIG_LOGIN_PHONE_MOCK, "false"))) {
            cache.setCacheObject(buildCacheKey(phone), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return numbers;
        }

        // 分支 A：应用自带 SMS 渠道，不计费
        if (map.containsKey(CONFIG_SMS_SUPPLIER)) {
            sendByAppConfig(map, phone, numbers);
            cache.setCacheObject(buildCacheKey(phone), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return "";
        }

        // 分支 B：平台默认渠道 → 真发 + 计费；任何付费失败直接抛错，不降级
        if (smsDefaults.isUsable()) {
            String clientIp = ServletUtils.getClientIp();
            if (!smsRateLimitService.tryAcquire(clientIp)) {
                throw new RateLimitException();
            }

            Long ownerId = resolveOwner(appId);
            BigDecimal unitPrice = smsDefaults.getUnitPrice();
            if (ownerId == null || !userBalanceService.checkBalance(ownerId, unitPrice)) {
                throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
            }

            sendByDefaults(phone, numbers);
            boolean deducted = userBalanceService.deductBalance(ownerId, unitPrice);
            if (!deducted) {
                log.error("SMS 已发送但扣费失败，回滚事务避免记账与扣费不一致, appId={}, ownerId={}", appId, ownerId);
                throw new BusinessException(ErrorCode.BALANCE_NOT_ENOUGH);
            }
            apiBillingService.recordBilling(appId, null, null, BILLING_MODEL_PER_USE, 1, unitPrice);
            cache.setCacheObject(buildCacheKey(phone), numbers, LOGIN_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
            return "";
        }

        // 分支 C：mock 未开 + 应用未自配 + 平台默认未启用 → 抛错（统一计费严格语义，避免泄露验证码）
        log.error("SMS 通道未配置：mock 未开启、应用未自配、平台默认未启用, appId={}", appId);
        throw new BusinessException("login.login.phone.sms");
    }

    public String getLoginCode(String phone) {
        return cache.getCacheObject(buildCacheKey(phone));
    }

    private void sendByAppConfig(Map<String, String> map, String phone, String numbers) {
        String accessKeyId = map.getOrDefault(CONFIG_SMS_APP_ID, "");
        String accessKeySecret = map.getOrDefault(CONFIG_SMS_SECRET, "");
        String signature = map.getOrDefault(CONFIG_SMS_SIGN_NAME, "北京跨赴科技");
        String templateId = map.getOrDefault(CONFIG_SMS_CODE_TEMPLATE, "SMS_487525448");
        String code = map.getOrDefault(CONFIG_SMS_PARAM_TEMPLATE, "code");
        String supplier = map.getOrDefault(CONFIG_SMS_SUPPLIER, "alibaba");
        dispatchSms(supplier, accessKeyId, accessKeySecret, signature, templateId, code, phone, numbers);
    }

    private void sendByDefaults(String phone, String numbers) {
        dispatchSms(
                smsDefaults.getSupplier(),
                smsDefaults.getAppId(),
                smsDefaults.getAppSecret(),
                smsDefaults.getSignName(),
                smsDefaults.getTemplateCode(),
                smsDefaults.getTemplateParam(),
                phone,
                numbers);
    }

    private void dispatchSms(String supplier, String accessKeyId, String accessKeySecret,
                             String signature, String templateId, String paramName,
                             String phone, String numbers) {
        SmsConfigRequest smsConfigRequest = SmsConfigRequest.builder()
                .configId(supplier)
                .accessKeyId(accessKeyId)
                .accessKeySecret(accessKeySecret)
                .signature(signature)
                .templateId(templateId)
                .templateName(paramName).build();
        SmsConfigFactory smsConfigFactory = new SmsConfigFactory();
        smsConfigFactory.getSmsConfig(supplier, smsConfigRequest);
        SEInitializer.initializer().fromConfig(new SmsConfig(), smsConfigFactory.getSupplierConfigList(supplier));
        SmsResponse smsResponse = SmsFactory.getSmsBlend(supplier).sendMessage(phone, numbers);
        if (ObjectUtils.isEmpty(smsResponse) || !smsResponse.isSuccess()) {
            log.error("SMS 发送失败, supplier={}, phone={}, templateId={}, signature={}, response={}",
                    supplier, phone, templateId, signature, smsResponse);
            throw new BusinessException("login.login.phone.sms");
        }
        log.info("SMS 发送成功, supplier={}, phone={}, response={}", supplier, phone, smsResponse);
    }

    private Long resolveOwner(String appId) {
        try {
            AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
            return appInfo == null ? null : appInfo.getOwner();
        } catch (Exception e) {
            log.warn("resolveOwner 失败, appId={}", appId, e);
            return null;
        }
    }

    private String buildCacheKey(String phone) {
        return CACHE_PREFIX + phone;
    }
}
