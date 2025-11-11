package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.ApiBillingRecord;

import java.math.BigDecimal;

public interface ApiBillingService extends IService<ApiBillingRecord> {

    /**
     * 记录API调用计费
     * @param appId 应用ID
     * @param apiId API市场ID
     * @param dynamicApiId 动态API配置ID
     * @param billingModel 计费模式 1:按次 2:按token
     * @param quantity 数量(按次为1,按token为实际token数)
     * @param unitPrice 单价
     * @return 计费记录
     */
    ApiBillingRecord recordBilling(String appId, Integer apiId, Integer dynamicApiId,
                                   Integer billingModel, Integer quantity, BigDecimal unitPrice);

    /**
     * 计算费用
     * @param billingModel 计费模式 1:按次 2:按token
     * @param quantity 数量
     * @param unitPrice 单价
     * @return 总费用
     */
    BigDecimal calculateAmount(Integer billingModel, Integer quantity, BigDecimal unitPrice);
}
