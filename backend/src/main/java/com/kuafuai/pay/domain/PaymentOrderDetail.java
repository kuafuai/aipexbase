package com.kuafuai.pay.domain;

import com.kuafuai.pay.enums.PayStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付订单详情
 */
@Data
public class PaymentOrderDetail {
    /**
     * 支付订单ID
     */
    private String paymentOrderId;
    /**
     * 订单ID
     */
    private String orderId;
    /**
     * 订单金额
     */
    private BigDecimal amount;
    /**
     * 支付状态
     */
    private PayStatus status;
    /**
     * 支付渠道
     */
    private String payChannel;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 支付说明
     */
    private String subject;
}
