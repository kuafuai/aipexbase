package com.kuafuai.pay.config;

import com.google.common.collect.Maps;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.util.SpringUtils;

import java.util.Map;

/**
 * Huawei Petal Pay 配置.
 * <p>
 * Config keys (from DynamicConfigBusinessService.getSystemConfig(appId)):
 * huawei.pay.sp-merc-no          — 服务商商户号 (= PetalPayConfig.callerId)
 * huawei.pay.sp-app-id           — 服务商 appId
 * huawei.pay.sub-merc-no         — 子商户号
 * huawei.pay.private-key         — 商户 RSA 私钥 (PKCS#8, PEM 去头去尾)
 * huawei.pay.auth-id             — 证书编号 id
 * huawei.pay.petalpay-public-key — 华为侧公钥, 验签用
 * huawei.pay.domain-host         — 网关 host, 默认沙箱
 * huawei.pay.biz-type            — 业务类型, 默认 100002
 */
public class HuaweiPayConfig {

    /**
     * 服务商商户号 callerId -> huawei.pay.sp-merc-no
     */
    private String spMercNo;

    /**
     * 服务商 appId -> huawei.pay.sp-app-id
     */
    private String spAppId;

    /**
     * 子商户号 -> huawei.pay.sub-merc-no
     */
    private String subMercNo;

    /**
     * 商户 RSA 私钥 -> huawei.pay.private-key
     */
    private String privateKey;

    /**
     * 证书编号 id -> huawei.pay.auth-id
     */
    private String authId;

    /**
     * 华为侧公钥 -> huawei.pay.petalpay-public-key
     */
    private String petalpayPublicKey;
    private String domainHost;
    private String bizType;

    private String orderPreKey;
    private String payBackUrl;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, String> parseMap = Maps.newHashMap();

        public Builder appId(String appId) {
            DynamicConfigBusinessService svc = SpringUtils.getBean(DynamicConfigBusinessService.class);
            Map<String, String> map = svc.getSystemConfig(appId);
            map.forEach(parseMap::putIfAbsent);
            return this;
        }

        public HuaweiPayConfig build() {
            HuaweiPayConfig c = new HuaweiPayConfig();
            c.spMercNo = parseMap.get("huawei.pay.sp-merc-no");
            c.spAppId = parseMap.get("huawei.pay.sp-app-id");
            c.subMercNo = parseMap.get("huawei.pay.sub-merc-no");
            c.privateKey = parseMap.get("huawei.pay.private-key");
            c.authId = parseMap.get("huawei.pay.auth-id");
            c.petalpayPublicKey = parseMap.get("huawei.pay.petalpay-public-key");
            c.domainHost = parseMap.getOrDefault("huawei.pay.domain-host", "https://petalpay-developer.cloud.huawei.com.cn");
            c.bizType = parseMap.getOrDefault("huawei.pay.biz-type", "100002");

            c.orderPreKey = parseMap.getOrDefault("wx.pay.order_pre_key", "xxxx");
            c.payBackUrl = parseMap.getOrDefault("wx.pay.pay-back-url", "xxxx");
            return c;
        }
    }

    public String getSpMercNo() {
        return spMercNo;
    }

    public String getSpAppId() {
        return spAppId;
    }

    public String getSubMercNo() {
        return subMercNo;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getAuthId() {
        return authId;
    }

    public String getPetalpayPublicKey() {
        return petalpayPublicKey;
    }

    public String getDomainHost() {
        return domainHost;
    }

    public String getBizType() {
        return bizType;
    }

    public String getOrderPreKey() {
        return orderPreKey;
    }

    public String getPayBackUrl() {
        return payBackUrl;
    }
}
