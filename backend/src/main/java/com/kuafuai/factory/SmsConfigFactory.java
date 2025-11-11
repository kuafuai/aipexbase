package com.kuafuai.factory;

import com.kuafuai.factory.sms.SmsSendMessage;
import com.kuafuai.factory.sms.impl.AlibabaSendMessage;
import com.kuafuai.factory.sms.impl.HuaweiSendMessage;
import com.kuafuai.factory.sms.impl.TencentSendMessage;
import com.kuafuai.factory.sms.request.SmsConfigRequest;
import org.dromara.sms4j.aliyun.config.AlibabaConfig;
import org.dromara.sms4j.api.universal.SupplierConfig;
import org.dromara.sms4j.comm.constant.SupplierConstant;
import org.dromara.sms4j.huawei.config.HuaweiConfig;
import org.dromara.sms4j.tencent.config.TencentConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SmsConfigFactory {

    private final Map<String, List<SupplierConfig>> blends = new ConcurrentHashMap<>();


    public SmsSendMessage getSmsConfig(String supplier, SmsConfigRequest smsConfigRequest) {

        if (SupplierConstant.ALIBABA.equalsIgnoreCase(supplier)) {
            AlibabaSendMessage alibabaSendMessage = new AlibabaSendMessage();
            alibabaSendMessage.buildSmsConfig(smsConfigRequest);
            AlibabaConfig alibabaConfig = alibabaSendMessage.getAlibabaConfig();
            ArrayList<SupplierConfig> supplierConfigs = new ArrayList<>();
            supplierConfigs.add(alibabaConfig);
            blends.put(supplier, supplierConfigs);
            return alibabaSendMessage;
        } else if (SupplierConstant.HUAWEI.equalsIgnoreCase(supplier)) {
            HuaweiSendMessage huaweiSendMessage = new HuaweiSendMessage();
            huaweiSendMessage.buildSmsConfig(smsConfigRequest);
            HuaweiConfig huaweiConfig = huaweiSendMessage.getHuaweiConfig();
            ArrayList<SupplierConfig> supplierConfigs = new ArrayList<>();
            supplierConfigs.add(huaweiConfig);
            blends.put(supplier, supplierConfigs);
            return huaweiSendMessage;
        } else if (SupplierConstant.TENCENT.equalsIgnoreCase(supplier)) {
            TencentSendMessage tencentSendMessage = new TencentSendMessage();
            tencentSendMessage.buildSmsConfig(smsConfigRequest);
            TencentConfig tencentConfig = tencentSendMessage.getTencentConfig();
            ArrayList<SupplierConfig> supplierConfigs = new ArrayList<>();
            supplierConfigs.add(tencentConfig);
            blends.put(supplier, supplierConfigs);
            return tencentSendMessage;
        } else {
            throw new IllegalArgumentException("不支持的短信供应商: " + supplier);
        }
    }

    public List<SupplierConfig> getSupplierConfigList(String supplier) {
        return blends.get(supplier);
    }
}
