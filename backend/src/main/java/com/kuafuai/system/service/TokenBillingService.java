package com.kuafuai.system.service;

import com.kuafuai.api.spec.TokenUsageReportRequest;

import java.math.BigDecimal;

public interface TokenBillingService {

    /**
     * 上报 token 用量并扣费。
     * 对每条 record 计算费用，批量写入 token_billing_record，汇总后扣减余额。
     *
     * @param request 上报请求，包含 codeflying_user_id、chat_id 和 records 列表
     * @return 本次扣减的总金额
     */
    BigDecimal reportAndCharge(TokenUsageReportRequest request);
}
