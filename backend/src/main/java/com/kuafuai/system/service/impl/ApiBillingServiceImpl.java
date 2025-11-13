package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ApiBillingRecord;
import com.kuafuai.system.mapper.ApiBillingRecordMapper;
import com.kuafuai.system.service.ApiBillingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Service
@Slf4j
public class ApiBillingServiceImpl extends ServiceImpl<ApiBillingRecordMapper, ApiBillingRecord> implements ApiBillingService {

    private static final int BILLING_MODEL_PER_USE = 1;     // 按次计费
    private static final int BILLING_MODEL_PER_TOKEN = 2;   // 按token计费
    private static final double DIVISOR_10000 = 10000.0;    // $.xx/1w
    private static final double MIN_CHARGE_0_01 = 0.01;     // 最低费用
    private static final BigDecimal MIN_CHARGE = BigDecimal.valueOf(MIN_CHARGE_0_01);
    private static final BigDecimal TOKEN_DIVISOR = BigDecimal.valueOf(DIVISOR_10000);
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

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
        if (!isValidInput(billingModel, unitPrice)) {
            return BigDecimal.ZERO;
        }

        switch (billingModel) {
            case BILLING_MODEL_PER_USE:
                return calculatePerUseAmount(unitPrice);
            case BILLING_MODEL_PER_TOKEN:
                return calculatePerTokenAmount(quantity, unitPrice);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculatePerUseAmount(BigDecimal unitPrice) {
        return unitPrice;
    }

    private BigDecimal calculatePerTokenAmount(Integer quantity, BigDecimal unitPrice) {
        if (!isValidQuantity(quantity)) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = calculateRawAmount(quantity, unitPrice);
        return ensureMinimumCharge(amount);
    }

    private BigDecimal calculateRawAmount(Integer quantity, BigDecimal unitPrice) {
        BigDecimal quantityBD = new BigDecimal(quantity);
        return unitPrice.multiply(quantityBD).divide(TOKEN_DIVISOR, SCALE, ROUNDING_MODE);
    }

    private BigDecimal ensureMinimumCharge(BigDecimal amount) {
        return amount.compareTo(MIN_CHARGE) < 0 ? MIN_CHARGE : amount;
    }

    private boolean isValidQuantity(Integer quantity) {
        return quantity != null && quantity > 0;
    }

    private boolean isValidInput(Integer billingModel, BigDecimal unitPrice) {
        return billingModel != null && unitPrice != null;
    }
}
