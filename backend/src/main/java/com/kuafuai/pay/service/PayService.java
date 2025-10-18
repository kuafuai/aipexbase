package com.kuafuai.pay.service;



import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import com.kuafuai.pay.domain.PaymentOrderDetail;
import com.kuafuai.pay.enums.PayStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 通用订单支付服务接口
 */
public interface PayService<T extends PaymentOrderDetail> {

    /**
     * 创建支付订单
     *
     * @param orderId     业务订单ID
     * @param amount      支付金额
     * @param subject     支付标题/商品描述
     * @param extraParams 额外参数
     * @param database
     * @return 支付订单ID
     */
    String createPaymentOrder(PayLoginVo login, String orderId, BigDecimal amount, String subject, Object extraParams, String database);


    Object getPaymentParam(String paymentOrderId, Object extraParams);

    /**
     * 查询支付状态
     *
     * @param paymentOrderId 支付订单ID
     * @return 支付状态枚举
     */
    PayStatus queryPaymentStatus(String paymentOrderId);

    /**
     * 取消支付订单
     *
     * @param paymentOrderId 支付订单ID
     * @return 是否取消成功
     */
    boolean cancelPaymentOrder(String paymentOrderId);

    /**
     * 申请退款
     *
     * @param paymentOrderId 支付订单ID
     * @param refundAmount   退款金额
     * @param reason         退款原因
     * @return 退款订单ID
     */
    String applyRefund(String refundOrderNo,String paymentOrderId, BigDecimal refundAmount, String reason, BigDecimal totalAmount);

    /**
     * 查询退款状态
     *
     * @param refundOrderId 退款订单ID
     * @return 退款状态
     */
    PayStatus queryRefundStatus(String refundOrderId);

    /**
     * 处理支付回调通知
     *
     * @param callbackData 回调数据
     * @return 处理结果
     */
    boolean processPaymentCallback(Object callbackData);


    boolean processRefundCallback(Object callbackData);

    /**
     * 关闭支付订单
     *
     * @param paymentOrderId 支付订单ID
     * @return 是否关闭成功
     */
    boolean closePaymentOrder(String paymentOrderId,String orderNo);

    /**
     * 获取支付订单详情
     *
     * @param paymentOrderId 支付订单ID
     * @return 支付订单详情
     */
    T getPaymentOrderDetail(String orderNo);

    /**
     * 支付回调成功的返回结果
     * @return
     */
    Object payCallbackProcessSuccess();

    /**
     * 支付回调失败的返回结果
     * @return
     */
    Object payCallbackProcessFail();

    /**
     * 支付回调数据解密
     *
     * @param data
     * @param headers
     * @return
     */

    PayCallbackRequest callbackDecryption(Object requestData, Map<String, String> headers);
}

