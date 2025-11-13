package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ApiBillingRecord;
import com.kuafuai.system.mapper.ApiBillingRecordMapper;
import com.kuafuai.system.service.ApiBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Service
@Slf4j
public class ApiBillingServiceImpl extends ServiceImpl<ApiBillingRecordMapper, ApiBillingRecord> implements ApiBillingService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApiBillingRecord recordBilling(String appId, Integer apiId, Integer dynamicApiId,
                                          Integer billingModel, Integer quantity, BigDecimal unitPrice) {
        BigDecimal totalAmount = calculateAmount(billingModel, quantity, unitPrice);

        ApiBillingRecord record = ApiBillingRecord.builder()
                .appId(appId)
                .apiId(apiId)
                .dynamicApiId(dynamicApiId)
                .billingModel(billingModel)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .totalAmount(totalAmount)
                .calledAt(new Date())
                .build();

        save(record);
        log.info("记录计费成功, appId: {}, apiId: {}, dynamicApiId: {}, billingModel: {}, quantity: {}, totalAmount: {}",
                appId, apiId, dynamicApiId, billingModel, quantity, totalAmount);

        return record;
    }

    @Override
    public BigDecimal calculateAmount(Integer billingModel, Integer quantity, BigDecimal unitPrice) {
        if (billingModel == null || unitPrice == null) {
            return BigDecimal.ZERO;
        }

        // 1: 按次收费, 2: 按token收费
        if (billingModel == 1) {
            // 按次收费: quantity固定为1
            return unitPrice;
        } else if (billingModel == 2) {
            // 按token收费: 费用 = quantity * unitPrice
            if (quantity == null || quantity <= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity/10000.0));
            // 如果按token计费的费用不足0.01，就以0.01计费
            if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
                return BigDecimal.valueOf(0.01);
            }
            return amount;
        }

        return BigDecimal.ZERO;
    }
}
