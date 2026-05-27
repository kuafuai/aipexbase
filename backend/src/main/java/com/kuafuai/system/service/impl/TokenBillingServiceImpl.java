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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

        List<TokenBillingRecord> billingRecords = new ArrayList<>();
        BigDecimal totalCharge = BigDecimal.ZERO;

        for (TokenUsageReportRequest.RecordItem item : request.getRecords()) {
            ModelPricing pricing = resolveModelPricing(item.getModel());
            if (pricing == null) {
                log.warn("未找到模型定价，跳过计费, model: {}, messageId: {}", item.getModel(), item.getMessageId());
                continue;
            }

            // cached tokens 已包含在 prompt_tokens 中，先扣除再补 cached 价格
            int nonCachedPromptTokens = safeInt(item.getPromptTokens()) - safeInt(item.getCachedTokens());
            if (nonCachedPromptTokens < 0) nonCachedPromptTokens = 0;

            BigDecimal promptAmount = calcTokenCost(nonCachedPromptTokens, pricing.getPromptUnitPrice());
            BigDecimal cacheAmount = calcTokenCost(safeInt(item.getCachedTokens()), pricing.getCacheUnitPrice());
            BigDecimal completionAmount = calcTokenCost(safeInt(item.getCompletionTokens()), pricing.getCompletionUnitPrice());
            BigDecimal recordTotal = promptAmount.add(cacheAmount).add(completionAmount);

            TokenBillingRecord record = TokenBillingRecord.builder()
                    .codeFlyingUserId(request.getCodeFlyingUserId())
                    .chatId(request.getChatId())
                    .messageId(item.getMessageId())
                    .agentId(item.getAgentId())
                    .model(item.getModel())
                    .promptTokens(item.getPromptTokens())
                    .completionTokens(item.getCompletionTokens())
                    .cachedTokens(item.getCachedTokens())
                    .totalTokens(item.getTotalTokens())
                    .promptAmount(promptAmount)
                    .completionAmount(completionAmount)
                    .cacheAmount(cacheAmount)
                    .totalAmount(recordTotal)
                    .messageTime(parseTimestamp(item.getTimestamp()))
                    .createdAt(new Date())
                    .build();

            billingRecords.add(record);
            totalCharge = totalCharge.add(recordTotal);
        }

        if (billingRecords.isEmpty()) {
            return BigDecimal.ZERO;
        }

        saveBatch(billingRecords);

        boolean deducted = userBalanceService.deductBalanceByCodeFlyingUserId(
                request.getCodeFlyingUserId(), totalCharge);
        if (!deducted) {
            log.error("扣减余额失败, codeFlyingUserId: {}, amount: {}", request.getCodeFlyingUserId(), totalCharge);
            throw new RuntimeException("余额不足或扣减失败");
        }

        log.info("token计费完成, codeFlyingUserId: {}, chatId: {}, records: {}, totalCharge: {}",
                request.getCodeFlyingUserId(), request.getChatId(), billingRecords.size(), totalCharge);

        return totalCharge;
    }

    private ModelPricing resolveModelPricing(String model) {
        if (StringUtils.isBlank(model)) return null;
        return modelPricingService.getByModelName(model);
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

    private Date parseTimestamp(String timestamp) {
        if (StringUtils.isBlank(timestamp)) return new Date();
        try {
            return Date.from(Instant.parse(timestamp));
        } catch (Exception e) {
            log.warn("解析 timestamp 失败: {}", timestamp);
            return new Date();
        }
    }
}
