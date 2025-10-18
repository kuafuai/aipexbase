package com.kuafuai.pay.domain;

import com.kuafuai.pay.enums.PayStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@ToString
@EqualsAndHashCode
public class PayCallbackRequest {

    /**
     * 支付系统中的订单id
     */
    private String paymentOrderId;

    /**
     * 本系统中的业务订单号
     */
    private String orderNo;
    /**
     * 支付时间
     */

    private LocalDateTime paymentTime;
    /**
     * 实际支付金额
     */

    private BigDecimal paymentAmount;

    /**
     * 支付状态
     */
    private PayStatus payStatus;

    /**
     * 额外的其他参数
     */
    private Map<String, Object> extraParamMap;


}
