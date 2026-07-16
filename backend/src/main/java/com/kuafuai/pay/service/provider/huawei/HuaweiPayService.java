package com.kuafuai.pay.service.provider.huawei;


import com.huawei.petalpay.paymentservice.apiservice.client.model.PreOrderCreateResponse;
import com.huawei.petalpay.paymentservice.core.client.DefaultPetalPayClient;
import com.huawei.petalpay.paymentservice.core.client.PetalPayClient;
import com.huawei.petalpay.paymentservice.core.config.PetalPayConfig;
import com.huawei.petalpay.paymentservice.core.constant.SignType;
import com.huawei.petalpay.paymentservice.apiservice.client.model.NotifyPaymentReq;
import com.huawei.petalpay.paymentservice.core.callback.CallBackBaseResponse;
import com.huawei.petalpay.paymentservice.core.tools.VerifyTools;

import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.common.dynamic_config.service.DynamicRefreshService;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.JSON;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.pay.business.domain.OrderCreatRequest;
import com.kuafuai.pay.config.HuaweiPayConfig;
import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import com.kuafuai.pay.domain.PaymentOrderDetail;
import com.kuafuai.pay.enums.PayStatus;
import com.kuafuai.pay.service.PayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Huawei Petal Pay 服务实现.
 * <p>
 * 流程:
 * 1. createPaymentOrder -> POST /api/v2/partner/aggr/preorder/create/fa 拿到 prepayId, 存入返回 map
 * 2. getPaymentParam(prepayId) -> payClient.buildOrderStr(prepayId), 返回给客户端拉起支付
 * 3. 华为支付完成后异步回调 payBackUrl + /{database}/huawei_petal
 * <p>
 * 目前状态 (P0):
 * - 创建订单 + 拉起参数 已实现
 * - 查询/取消/退款/回调解密 均为 TODO, 等 SDK 里对应模型确认后再补
 */
@Component("huawei")
@Scope("prototype")
@Slf4j
public class HuaweiPayService implements PayService<PaymentOrderDetail>, DynamicRefreshService {

    protected HuaweiPayConfig huaweiPayConfig;

    private PetalPayClient payClient;

    public HuaweiPayService() {
        final String appId = ConfigContext.getDatabase();
        this.huaweiPayConfig = HuaweiPayConfig.builder().appId(appId).build();
        refresh();
    }

    @Override
    public void refresh() {
        log.info("HuaweiPayService refresh 配置");
        if (StringUtils.isEmpty(huaweiPayConfig.getSpMercNo()) || StringUtils.isEmpty(huaweiPayConfig.getSpAppId()) || StringUtils.isEmpty(huaweiPayConfig.getPrivateKey())) {
            log.warn("HuaweiPayService 配置不完整, 跳过 client 初始化");
            return;
        }

        final PetalPayConfig config = PetalPayConfig.builder()
                .callerId(huaweiPayConfig.getSpMercNo())
                .appId(huaweiPayConfig.getSpAppId())
                .privateKey(huaweiPayConfig.getPrivateKey())
                .authId(huaweiPayConfig.getAuthId())
                .petalpayPublicKey(huaweiPayConfig.getPetalpayPublicKey())
                .signType(SignType.RSA)
                .domainHost(huaweiPayConfig.getDomainHost())
                .build();
        this.payClient = new DefaultPetalPayClient(config);
    }

    @Override
    public Map<String, Object> createPaymentOrder(PayLoginVo login, String orderId, BigDecimal amount,
                                                  String subject, OrderCreatRequest extraParams, String database) {

        // 华为侧金额单位 = 分
        final long totalFen = amount.multiply(new BigDecimal("100")).longValue();

        final String mercOrderNo = processOrderNo(orderId);
        final String callbackUrl = huaweiPayConfig.getPayBackUrl() + "/" + database + "/huawei";

        final PreOrderCreateRequestV3 request = PreOrderCreateRequestV3.builder()
                .mercOrderNo(mercOrderNo)
                .spAppId(huaweiPayConfig.getSpAppId())
                .spMercNo(huaweiPayConfig.getSpMercNo())
                .subMercNo(huaweiPayConfig.getSubMercNo())
                .tradeSummary(subject)
                .bizType(huaweiPayConfig.getBizType())
                .totalAmount(totalFen)
                .callbackUrl(callbackUrl)
                .build();

        final PreOrderCreateResponse response;
        try {
            response = payClient.execute("POST",
                    "/api/v2/partner/aggr/preorder/create/fa",
                    PreOrderCreateResponse.class,
                    request);
        } catch (Exception e) {
            log.error("华为支付预下单异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "华为支付预下单失败");
        }

        if (response == null || !"000000".equals(response.getResultCode())) {
            log.error("华为支付预下单失败, response={}", response);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "华为支付预下单失败");
        }

        final String prepayId = response.getPrepayId();
        if (StringUtils.isEmpty(prepayId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "预支付订单生成失败, 请重试");
        }

        final Map<String, Object> map = new HashMap<>();
        map.put("prepay_id", prepayId);
        map.put("orderNo", orderId);
        map.put("orderStr", payClient.buildOrderStr(prepayId));
        return map;
    }

    @Override
    public String getPayId(Map<String, Object> param) {
        return (String) param.getOrDefault("prepay_id", "");
    }

    @Override
    public Object getPaymentParam(String prepayId, Map<String, Object> extraParams) {
        try {
            final String orderStr = payClient.buildOrderStr(prepayId);
            final Map<String, Object> result = new HashMap<>();
            result.put("orderStr", orderStr);
            result.put("prepayId", prepayId);
            result.put("orderNo", extraParams.getOrDefault("orderNo", "").toString());
            return result;
        } catch (Exception e) {
            log.error("华为支付 buildOrderStr 异常", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "华为支付参数生成失败");
        }
    }

    @Override
    public PayStatus queryPaymentStatus(String paymentOrderId) {
        // TODO P1: 调华为查询接口, 映射到 PayStatus
        log.warn("HuaweiPayService.queryPaymentStatus 未实现, paymentOrderId={}", paymentOrderId);
        return null;
    }

    @Override
    public boolean cancelPaymentOrder(String paymentOrderId) {
        // TODO P1
        return false;
    }

    @Override
    public String applyRefund(String refundOrderNo, String paymentOrderId, BigDecimal refundAmount,
                              String reason, BigDecimal totalAmount) {
        // TODO P2: 华为退款接口
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "华为支付退款暂未支持");
    }

    @Override
    public PayStatus queryRefundStatus(String refundOrderId) {
        // TODO P2
        return null;
    }

    @Override
    public boolean processPaymentCallback(Object callbackData) {
        // 回调实际业务逻辑走 GeneralOrderBusinessController -> OrderFacadeService, 这里默认放行
        return true;
    }

    @Override
    public boolean processRefundCallback(Object callbackData) {
        return true;
    }

    @Override
    public boolean closePaymentOrder(String paymentOrderId, String orderNo) {
        // TODO P1
        return false;
    }

    @Override
    public PaymentOrderDetail getPaymentOrderDetail(String orderNo) {
        // TODO P1: 走 queryPaymentStatus 的同一接口
        return null;
    }

    @Override
    public Object payCallbackProcessSuccess() {
        // 华为要求回调应答 JSON: { "resultCode": "000000", "resultDesc": "success" }
        final Map<String, Object> resp = new HashMap<>();
        resp.put("resultCode", "000000");
        resp.put("resultDesc", "success");
        return resp;
    }

    @Override
    public Object payCallbackProcessFail() {
        final Map<String, Object> resp = new HashMap<>();
        resp.put("resultCode", "999999");
        resp.put("resultDesc", "fail");
        return resp;
    }

    @Override
    public PayCallbackRequest callbackDecryption(Object requestData, Map<String, String> headers, String database) {
        log.info("HuaweiPayService.callbackDecryption 未实现, data={}, headers={}", requestData, headers);
        String publicKey = huaweiPayConfig.getPetalpayPublicKey();
        VerifyTools.getCallbackResult((String) requestData, publicKey, reqString -> {
            log.info("======={}", reqString);
            NotifyPaymentReq callbackReq = JSON.parseObject(reqString, NotifyPaymentReq.class);
            // 商户自行业务处理
            // doProcess(callbackReq);
        });
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "华为支付回调解密暂未支持");
    }

    private String processOrderNo(String orderNo) {
        if (orderNo.length() < 6) {
            return huaweiPayConfig.getOrderPreKey() + orderNo;
        }
        return orderNo;
    }
}
