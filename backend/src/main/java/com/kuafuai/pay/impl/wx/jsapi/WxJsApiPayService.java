package com.kuafuai.pay.impl.wx.jsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.common.dynamic_config.service.DynamicRefreshService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.RandomStringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.pay.config.WxV3PayConfig;
import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import com.kuafuai.pay.domain.PaymentOrderDetail;
import com.kuafuai.pay.enums.PayStatus;
import com.kuafuai.pay.service.PayService;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.partnerpayments.jsapi.model.Transaction;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.TransactionAmount;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component("wx_jsapi")
@Slf4j
//@DynamicRefreshScopeAnnotation
@Scope("prototype")
public class WxJsApiPayService implements PayService<PaymentOrderDetail>, DynamicRefreshService {
    private JsapiService service;

    protected WxV3PayConfig wxV3PayConfig;

    private NotificationConfig notificationConfig;

    private RefundService refundService;


    @Resource
    private ObjectMapper objectMapper;





    public WxJsApiPayService() {
        this.wxV3PayConfig = WxV3PayConfig.builder()
                .appId(ConfigContext.getDatabase())
                .build();
        refresh();
    }

    @Override
    public void refresh() {
        log.info("refresh 配置");
        if (wxV3PayConfig.getWxEnable()){
            final RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                    .merchantId(wxV3PayConfig.getMchId())
                    .privateKey(wxV3PayConfig.getPrivateKey())
                    .merchantSerialNumber(wxV3PayConfig.getMchSerialNo())
                    .apiV3Key(wxV3PayConfig.getApiV3Key())
                    .build();


            notificationConfig = new RSAAutoCertificateConfig.Builder()
                    .merchantId(wxV3PayConfig.getMchId())
                    .privateKey(wxV3PayConfig.getPrivateKey())
                    .merchantSerialNumber(wxV3PayConfig.getMchSerialNo())
                    .apiV3Key(wxV3PayConfig.getApiV3Key())
                    .build();


            refundService = new RefundService.Builder().config(config).build();
            // request.setXxx(val)设置所需参数，具体参数可见Request定义
            // 构建service,用于处理JSAPI支付相关的操作

            service = new JsapiService.Builder().config(config).build();
        }
    }

    @Override
    public String createPaymentOrder(PayLoginVo login, String orderId, BigDecimal amount, String subject, Object extraParams, String database) {
        //   进行分的转化
        final Boolean test = wxV3PayConfig.getTest();
        if (test){
            final String isTestAmount = wxV3PayConfig.getIsTestAmount();
            amount=new BigDecimal(isTestAmount);
        }
        final BigDecimal multiplicand = new BigDecimal("100");
        BigDecimal multiply = amount.multiply(multiplicand);
        PrepayRequest request = new PrepayRequest();
        Amount amountRequest = new Amount();
        amountRequest.setTotal(multiply.intValue()); // 已分为单位
        request.setAmount(amountRequest);
        request.setAppid(getAppId());
        request.setMchid(wxV3PayConfig.getMchId());
        request.setDescription(subject);
        request.setNotifyUrl(wxV3PayConfig.getPayBackUrl() +"/"+ database + "/wx_jsapi");

        request.setOutTradeNo(processOrderNo(orderId));

        final Payer payer = new Payer();
        payer.setOpenid(login.getWxOpenId());
        request.setPayer(payer);

        PrepayResponse prepay = service.prepay(request);

        final String prepayId = prepay.getPrepayId();
        if (StringUtils.isEmpty(prepayId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "预支付订单生成失败，请重试");
        }
        return prepayId;
    }

    @Override
    public Object getPaymentParam(String prepayId, Object extraParams) {
        final long timeStamp = new Date().getTime();
        final String nonceStr = RandomStringUtils.generateRandomString(16);
        StringBuilder sb = new StringBuilder();
        // 应用id
        sb.append(getAppId()).append("\n");
        // 支付签名时间戳
        sb.append(timeStamp).append("\n");
        // 随机字符串
        sb.append(nonceStr).append("\n");
        // 预支付交易会话ID
        sb.append("prepay_id=").append(prepayId).append("\n");
        try {
            // 签名
            Signature sign = Signature.getInstance("SHA256withRSA");
            // 获取商户私钥并进行签名
            final PrivateKey privateKey = wxV3PayConfig.readPrivateKeyContent(wxV3PayConfig.getPrivateKey());
//            // 将私钥内容转换为字节数组

            sign.initSign(privateKey);

            sign.update(sb.toString().getBytes("utf-8"));
            String paySign = Base64.getEncoder().encodeToString(sign.sign());

            final HashMap<String, Object> result = new HashMap<>();

            result.put("appId", getAppId());
            result.put("timeStamp", String.valueOf(timeStamp));
            result.put("nonceStr", nonceStr);
            result.put("prepayId", prepayId);
            result.put("signType", "RSA");
            result.put("paySign", paySign);
            result.put("package", "prepay_id=" + prepayId);
            return result;
        } catch (InvalidKeyException | UnsupportedEncodingException | NoSuchAlgorithmException | SignatureException e) {

            throw new BusinessException(ErrorCode.OPERATION_ERROR, "密钥加载失败");
        }
    }

    @Override
    public PayStatus queryPaymentStatus(String paymentOrderId) {
        final PaymentOrderDetail paymentOrderDetail = this.getPaymentOrderDetail(paymentOrderId);
        if (paymentOrderDetail == null){
            return null;
        }
        return paymentOrderDetail.getStatus();
    }

    @Override
    public boolean cancelPaymentOrder(String paymentOrderId) {
        return false;
    }

    @Override
    public String applyRefund(String refundOrderNo,String paymentOrderId, BigDecimal refundAmount, String reason, BigDecimal totalAmount) {
        if (wxV3PayConfig.getTest()){
            refundAmount=new BigDecimal(wxV3PayConfig.getIsTestAmount());
            totalAmount=new BigDecimal(wxV3PayConfig.getIsTestAmount());
        }
        BigDecimal multiplicand = new BigDecimal("100");
        final CreateRequest request = new CreateRequest();
//        request.setSubMchid(wxV3PayConfig.getMchId())
        request.setTransactionId(paymentOrderId);

        request.setOutRefundNo(refundOrderNo); //
        request.setReason(reason);

        request.setNotifyUrl(wxV3PayConfig.getPayBackUrl() + GlobalAppIdFilter.getAppId()+ "/refund/wx_jsapi");
        final AmountReq amount = new AmountReq();
        amount.setTotal(totalAmount.multiply(multiplicand).longValue());
        amount.setRefund(refundAmount.multiply(multiplicand).longValue());
        amount.setCurrency("CNY");
        request.setAmount(amount);

        final Refund refund = refundService.create(request);
        log.info("refund result:{}", refund);
        return refund.getRefundId();

    }

    @Override
    public PayStatus queryRefundStatus(String refundOrderId) {
        return null;
    }

    @Override
    public boolean processPaymentCallback(Object callbackData) {
        return false;
    }

    @Override
    public boolean processRefundCallback(Object callbackData) {
        return false;
    }

    @Override
    public boolean closePaymentOrder(String paymentOrderId, String orderNo) {
        final CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(wxV3PayConfig.getMchId());
        request.setOutTradeNo(processOrderNo(orderNo));
        try {
            service.closeOrder(request);
        } catch (ServiceException e) {

            final String responseBody = e.getResponseBody();
            if (StringUtils.containsAnyIgnoreCase(responseBody, "ORDERPAID")) {
                return false;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单关闭失败");
        }


        return true;


    }

    private String processOrderNo(String orderNo) {
        if (orderNo.length() < 6) {
            return wxV3PayConfig.getOrderPreKey() + orderNo;
        }
        return orderNo;
    }

    @Override
    public PaymentOrderDetail getPaymentOrderDetail(String orderNo) {


        final QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(wxV3PayConfig.getMchId());
        request.setOutTradeNo(processOrderNo(orderNo));
        final com.wechat.pay.java.service.payments.model.Transaction transaction = service.queryOrderByOutTradeNo(request);

        final PaymentOrderDetail paymentOrderDetail = new PaymentOrderDetail();

        paymentOrderDetail.setPaymentOrderId(transaction.getTransactionId());
        final String outTradeNo = transaction.getOutTradeNo();
        if (outTradeNo != null) {
            if (outTradeNo.startsWith(wxV3PayConfig.getOrderPreKey())) {
                paymentOrderDetail.setOrderId(outTradeNo.substring(wxV3PayConfig.getOrderPreKey().length()));
            } else {
                paymentOrderDetail.setOrderId(outTradeNo);
            }

        }
        final PayStatus payStatus = getPayStatus(transaction);
        final TransactionAmount amount = transaction.getAmount();
        final Integer total = amount.getTotal();
        paymentOrderDetail.setAmount(new BigDecimal(total).divide(new BigDecimal(100)));
        paymentOrderDetail.setStatus(payStatus);
        paymentOrderDetail.setPayChannel("wx_jsapi");
        if (StringUtils.isNotEmpty(transaction.getSuccessTime())){
            paymentOrderDetail.setPayTime(getLocalDateTime(transaction.getSuccessTime()));
        }


        return paymentOrderDetail;


    }

    @Override
    public Object payCallbackProcessSuccess() {
        return null;
    }

    @Override
    public Object payCallbackProcessFail() {
        final HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        objectObjectHashMap.put("code", "FAIL");
        objectObjectHashMap.put("message", "调用失败");
        return objectObjectHashMap;
    }

    @Override
    public PayCallbackRequest callbackDecryption(Object requestData, Map<String, String> headers) {
        NotificationParser parser = new NotificationParser(notificationConfig);

        String wechatTimestamp = headers.get("Wechatpay-Timestamp");
        if (StringUtils.isEmpty(wechatTimestamp)) {
            wechatTimestamp = headers.get("wechatpay-timestamp");
        }
        String wechatpayNonce = headers.get("Wechatpay-Nonce");
        if (StringUtils.isEmpty(wechatpayNonce)) {
            wechatpayNonce = headers.get("wechatpay-nonce");
        }
        String wechatSignature = headers.get("Wechatpay-Signature");
        if (StringUtils.isEmpty(wechatSignature)) {
            wechatSignature = headers.get("wechatpay-signature");
        }
        String wechatPaySerial = headers.get("Wechatpay-Serial");
        if (StringUtils.isEmpty(wechatPaySerial)) {
            wechatPaySerial = headers.get("wechatpay-serial");
        }

        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(wechatPaySerial)
                .nonce(wechatpayNonce)
                .signature(wechatSignature)
                .timestamp(wechatTimestamp)
                .body(String.valueOf(requestData))
                .build();

        // 以支付通知回调为例，验签、解密并转换成 Transaction
        Transaction transaction = parser.parse(requestParam, Transaction.class);

        final PayStatus payStatus = getPayStatus(transaction);
        PayCallbackRequest payCallbackRequest = new PayCallbackRequest();
        payCallbackRequest.setPaymentOrderId(transaction.getTransactionId());
        final String outTradeNo = transaction.getOutTradeNo();
        if (outTradeNo.startsWith(wxV3PayConfig.getOrderPreKey())) {
            final String substring = outTradeNo.substring(wxV3PayConfig.getOrderPreKey().length()
            );
            payCallbackRequest.setOrderNo(substring);
        } else {
            payCallbackRequest.setOrderNo(outTradeNo);
        }
        payCallbackRequest.setPayStatus(payStatus);
        if (transaction.getSuccessTime() != null) {
            final LocalDateTime localDateTime = getLocalDateTime(transaction.getSuccessTime());

            payCallbackRequest.setPaymentTime(localDateTime);
        }

        final Integer total = transaction.getAmount().getTotal();
        if (total != null) {
            final BigDecimal divide = new BigDecimal(total).divide(new BigDecimal(100));
            payCallbackRequest.setPaymentAmount(divide); // 支付金额
        }


        Map<String, Object> map = objectMapper.convertValue(transaction, Map.class);

        payCallbackRequest.setExtraParamMap(map);

        return payCallbackRequest;


    }

    private static LocalDateTime getLocalDateTime(String successTime) {
        // 1. 先解析成 OffsetDateTime（带时区）
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(successTime);

        // 2. 转换为 LocalDateTime（去掉时区）
        LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
        return localDateTime;
    }

    @Nullable
    private static PayStatus getPayStatus(Transaction transaction) {
        PayStatus payStatus = null;
        final Transaction.TradeStateEnum tradeState = transaction.getTradeState();
        if (tradeState == Transaction.TradeStateEnum.SUCCESS) {
            payStatus = PayStatus.PAID_SUCCESS;
        } else if (tradeState == Transaction.TradeStateEnum.NOTPAY) {
            payStatus = PayStatus.UNPAID;
        } else if (tradeState == Transaction.TradeStateEnum.CLOSED) {
            payStatus = PayStatus.CLOSED;
        } else if (tradeState == Transaction.TradeStateEnum.REFUND) {
            payStatus = PayStatus.REFUNDED;
        }
        return payStatus;
    }

    @Nullable
    private static PayStatus getPayStatus(com.wechat.pay.java.service.payments.model.Transaction transaction) {
        PayStatus payStatus = null;
        final com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum tradeState = transaction.getTradeState();
//        final Transaction.TradeStateEnum tradeState = (Transaction.TradeStateEnum) tradeState1;
        if (tradeState == com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.SUCCESS) {
            payStatus = PayStatus.PAID_SUCCESS;
        } else if (tradeState == com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.NOTPAY) {
            payStatus = PayStatus.UNPAID;
        } else if (tradeState == com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.CLOSED) {
            payStatus = PayStatus.CLOSED;
        } else if (tradeState == com.wechat.pay.java.service.payments.model.Transaction.TradeStateEnum.REFUND) {
            payStatus = PayStatus.REFUNDED;
        }
        return payStatus;
    }

    public String getAppId(){
        return wxV3PayConfig.getAppId();
    }


}
