package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.ModelPricing;
import com.kuafuai.system.entity.TokenBillingRecord;
import com.kuafuai.system.entity.vo.TokenUsageReportRequest;
import com.kuafuai.system.mapper.TokenBillingRecordMapper;
import com.kuafuai.system.service.ModelPricingService;
import com.kuafuai.system.service.TokenBillingService;
import com.kuafuai.system.service.UserBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class TokenBillingServiceImpl extends ServiceImpl<TokenBillingRecordMapper, TokenBillingRecord> implements TokenBillingService {

    private static final BigDecimal TOKEN_DIVISOR = BigDecimal.valueOf(10000);
    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Autowired
    private ModelPricingService modelPricingService;

    @Autowired
    private UserBalanceService userBalanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal reportAndCharge(TokenUsageReportRequest request) {
        if (CollectionUtils.isEmpty(request.getRecords())) {
            return BigDecimal.ZERO;
        }

        // 按 model 合并 token 消耗
        Map<String, MergedTokens> mergedByModel = new LinkedHashMap<>();
        for (TokenUsageReportRequest.RecordItem item : request.getRecords()) {
            if (StringUtils.isBlank(item.getModel())) continue;
            mergedByModel.compute(item.getModel(), (m, acc) -> {
                if (acc == null) acc = new MergedTokens();
                acc.promptTokens += safeInt(item.getPromptTokens());
                acc.completionTokens += safeInt(item.getCompletionTokens());
                acc.cachedTokens += safeInt(item.getCachedTokens());
                acc.totalTokens += safeInt(item.getTotalTokens());
                return acc;
            });
        }

        List<TokenBillingRecord> billingRecords = new ArrayList<>();
        BigDecimal totalCharge = BigDecimal.ZERO;

        for (Map.Entry<String, MergedTokens> entry : mergedByModel.entrySet()) {
            String model = entry.getKey();
            MergedTokens merged = entry.getValue();

            ModelPricing pricing = modelPricingService.getByModelName(model);
            if (pricing == null) {
                log.warn("未找到模型定价，跳过计费, model: {}", model);
                continue;
            }

            // prompt、cached、completion 各自独立按对应单价计费
            BigDecimal promptAmount = calcTokenCost(merged.promptTokens, pricing.getPromptUnitPrice());
            BigDecimal cacheAmount = calcTokenCost(merged.cachedTokens, pricing.getCacheUnitPrice());
            BigDecimal completionAmount = calcTokenCost(merged.completionTokens, pricing.getCompletionUnitPrice());
            BigDecimal recordTotal = promptAmount.add(cacheAmount).add(completionAmount);

            billingRecords.add(TokenBillingRecord.builder()
                    .codeFlyingUserId(request.getCodeFlyingUserId())
                    .chatId(request.getChatId())
                    .model(model)
                    .promptTokens(merged.promptTokens)
                    .completionTokens(merged.completionTokens)
                    .cachedTokens(merged.cachedTokens)
                    .totalTokens(merged.totalTokens)
                    .promptAmount(promptAmount)
                    .completionAmount(completionAmount)
                    .cacheAmount(cacheAmount)
                    .totalAmount(recordTotal)
                    .createdAt(new Date())
                    .build());

            totalCharge = totalCharge.add(recordTotal);
        }

        if (billingRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if (!userBalanceService.checkBalanceByCodeFlyingUserId(request.getCodeFlyingUserId())) {
            log.warn("余额不足，拒绝扣费, codeFlyingUserId: {}", request.getCodeFlyingUserId());
            throw new RuntimeException("余额不足");
        }

        saveBatch(billingRecords);

        boolean deducted = userBalanceService.deductBalanceByCodeFlyingUserId(request.getCodeFlyingUserId(), totalCharge);
        if (!deducted) {
            log.error("扣减余额失败, codeFlyingUserId: {}, amount: {}", request.getCodeFlyingUserId(), totalCharge);
            throw new RuntimeException("余额不足或扣减失败");
        }

        log.info("token计费完成, codeFlyingUserId: {}, chatId: {}, models: {}, totalCharge: {}", request.getCodeFlyingUserId(), request.getChatId(), mergedByModel.keySet(), totalCharge);

        return totalCharge;
    }

    private static class MergedTokens {
        int promptTokens;
        int completionTokens;
        int cachedTokens;
        int totalTokens;
    }

    private BigDecimal calcTokenCost(int tokens, BigDecimal unitPrice) {
        if (tokens <= 0 || unitPrice == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(tokens)
                .multiply(unitPrice)
                .divide(TOKEN_DIVISOR, SCALE, ROUNDING_MODE);
    }

    private int safeInt(Integer val) {
        return val == null ? 0 : val;
    }

    @Override
    public boolean hasBalance(String codeFlyingUserId) {
        return userBalanceService.checkBalanceByCodeFlyingUserId(codeFlyingUserId);
    }
}
