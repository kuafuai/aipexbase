package com.kuafuai.system.service;

import com.kuafuai.system.entity.vo.TokenUsageReportRequest;

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
    /**
     * 检查用户余额是否大于 0
     *
     * @param codeFlyingUserId codeflying 用户ID
     * @return true=有余额，false=余额不足或账户不存在
     */
    boolean hasBalance(String codeFlyingUserId);

}
