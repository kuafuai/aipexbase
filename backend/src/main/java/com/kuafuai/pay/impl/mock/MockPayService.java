package com.kuafuai.pay.impl.mock;

import com.kuafuai.common.util.UUID;
import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import com.kuafuai.pay.domain.PaymentOrderDetail;
import com.kuafuai.pay.enums.PayStatus;
import com.kuafuai.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟支付服务实现
 * 适用于开发和测试环境
 */
@Component("mock")
//@DynamicRefreshScopeAnnotation
@Scope("prototype")
@Slf4j
public class MockPayService implements PayService<PaymentOrderDetail> {

    // 模拟存储支付订单
    private final Map<String, PaymentOrderDetail> mockOrderDB = new ConcurrentHashMap<>();
    // 模拟存储退款订单
    private final Map<String, MockRefundRecord> mockRefundDB = new ConcurrentHashMap<>();

    public MockPayService() {
        log.info("init");
    }

    @Override
    public String createPaymentOrder(PayLoginVo login, String orderId, BigDecimal amount, String subject, Object extraParams, String database) {
        String paymentOrderId = UUID.randomUUID().toString();
        PaymentOrderDetail order = new PaymentOrderDetail();
        order.setPaymentOrderId(paymentOrderId);
        order.setOrderId(orderId);
        order.setAmount(amount);
        order.setSubject(subject);
        order.setStatus(PayStatus.UNPAID);
        order.setCreateTime(LocalDateTime.now());

        mockOrderDB.put(paymentOrderId, order);
        return paymentOrderId;
    }

    @Override
    public Object getPaymentParam(String paymentOrderId, Object extraParams) {
        PaymentOrderDetail order = mockOrderDB.get(paymentOrderId);
        if (order == null) {
            throw new IllegalArgumentException("支付订单不存在");
        }

        // 模拟微信支付参数
        Map<String, String> mockParams = new HashMap<>();
        mockParams.put("appId", "mock_appid");
        mockParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
        mockParams.put("nonceStr", UUID.randomUUID().toString());
        mockParams.put("package", "prepay_id=MOCK_" + UUID.randomUUID());
        mockParams.put("signType", "MD5");
        mockParams.put("paySign", "MOCK_SIGN_" + UUID.randomUUID());
        mockParams.put("paymentOrderId", paymentOrderId);


        return mockParams;
    }

    @Override
    public PayStatus queryPaymentStatus(String paymentOrderId) {
        PaymentOrderDetail order = mockOrderDB.get(paymentOrderId);
        return order != null ? order.getStatus() : null;
    }

    @Override
    public boolean cancelPaymentOrder(String paymentOrderId) {
        PaymentOrderDetail order = mockOrderDB.get(paymentOrderId);
        if (order != null && order.getStatus() == PayStatus.UNPAID) {
            order.setStatus(PayStatus.CANCELLED);
            return true;
        }
        return false;
    }

    @Override
    public String applyRefund(String refundOrderNo,String paymentOrderId, BigDecimal refundAmount, String reason, BigDecimal totalAmount)
    {
        PaymentOrderDetail order = mockOrderDB.get(paymentOrderId);
        if (order == null) {
            throw new IllegalArgumentException("支付订单不存在");
        }

        String refundId = "REFUND_" + UUID.randomUUID();
        MockRefundRecord refund = new MockRefundRecord();
        refund.setRefundId(refundId);
        refund.setPaymentOrderId(paymentOrderId);
        refund.setRefundAmount(refundAmount);
        refund.setReason(reason);
        refund.setStatus(PayStatus.REFUNDED);
        refund.setCreateTime(LocalDateTime.now());

        mockRefundDB.put(refundId, refund);

        // 更新原订单状态
        order.setStatus(PayStatus.REFUNDED);

        return refundId;
    }

    @Override
    public PayStatus queryRefundStatus(String refundOrderId) {
        MockRefundRecord refund = mockRefundDB.get(refundOrderId);
        return refund != null ? refund.getStatus() : null;
    }

    @Override
    public boolean processPaymentCallback(Object callbackData) {
        // 模拟回调处理
        return true;
    }

    @Override
    public boolean processRefundCallback(Object callbackData) {
        // 模拟退款回调处理
        return true;
    }

    @Override
    public boolean closePaymentOrder(String paymentOrderId,String orderNo) {
        PaymentOrderDetail order = mockOrderDB.get(paymentOrderId);
        if (order != null && (order.getStatus() == PayStatus.UNPAID || order.getStatus() == PayStatus.PAYING)) {
            order.setStatus(PayStatus.CLOSED);
            return true;
        }
        return false;
    }

    @Override
    public PaymentOrderDetail getPaymentOrderDetail(String paymentOrderId) {
        return mockOrderDB.get(paymentOrderId);
    }

    @Override
    public Object payCallbackProcessSuccess() {

        return "success";

    }

    @Override
    public Object payCallbackProcessFail() {

        return "fail";
    }


    @Override
    public PayCallbackRequest callbackDecryption(Object requestData, Map<String, String> headers){
        // 这里是对回调数据进行解密的逻辑，可以根据你的业务需求进行修改
        final PayCallbackRequest payCallbackRequest = new PayCallbackRequest();
        payCallbackRequest.setPaymentOrderId(String.valueOf(requestData).replaceAll("\"",  "")); // 这里假设 requestData 需要转换为支付订单 ID
        payCallbackRequest.setOrderNo(null);  // 其他字段根据实际情况填充
        payCallbackRequest.setPaymentTime(LocalDateTime.now());
        payCallbackRequest.setPaymentAmount(null); // 可以从解密后的数据中填充
        payCallbackRequest.setExtraParamMap(null); // 可选字段

        return payCallbackRequest;  // 返回解密后的回调请求对象
    }


    /**
     * 模拟退款记录
     */
    private static class MockRefundRecord {
        private String refundId;
        private String paymentOrderId;
        private BigDecimal refundAmount;
        private String reason;
        private PayStatus status;
        private LocalDateTime createTime;

        // getters and setters
        public String getRefundId() {
            return refundId;
        }

        public void setRefundId(String refundId) {
            this.refundId = refundId;
        }

        public String getPaymentOrderId() {
            return paymentOrderId;
        }

        public void setPaymentOrderId(String paymentOrderId) {
            this.paymentOrderId = paymentOrderId;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public PayStatus getStatus() {
            return status;
        }

        public void setStatus(PayStatus status) {
            this.status = status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
}