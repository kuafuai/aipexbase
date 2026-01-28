package com.kuafuai.pay.business;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.service.LoginService;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.pay.business.domain.OrderCreatRequest;
import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class OrderFacadeService {

    @Resource
    private GeneralOrderBusinessService orderBusinessService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private TokenService tokenService;

    @Resource
    private LoginService loginService;

    public BaseResponse<?> handleOrder(String database, String operateName, Map<String, Object> body) {
        try {
            switch (operateName) {
                // 使用 product_id + user_id 生成唯一支付单 id
                case "getUniqueOrderNo":
                    return getUniqueOrderNo(body, database);
                // 创建支付单
                case "create":
                    return createOrder(body, database);
                case "deliver":
                    return deliverOrder(body, database);
                case "confirm":
                    return confirmOrder(body, database);
                case "getPaymentParam":
                    return getPaymentParam(body, database);
                // 查询支付单状态
                case "getPayOrderMessage":
                    return getPayOrderMessage(body, database);
                case "cancelPay":
                    return cancelPay(body, database);
                case "refund":
                    return refund(database,body);
                default:
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的操作类型: " + operateName);
            }
        } catch (Exception e) {
            log.error("操作处理失败: {}", operateName, e);
            throw e;
        }
    }

    private BaseResponse<?> refund(String databaae,Map<String, Object> body) {
        return ResultUtils.success(orderBusinessService.refund(databaae,String.valueOf(body.get("id"))));
    }

    private BaseResponse<?> cancelPay(Map<String, Object> body, String database) {
        Object deliverOrderNo = body.get("orderNo");

        if (ObjectUtils.isEmpty(deliverOrderNo)) {

            deliverOrderNo = body.get("paymentOrderId");
        }


        return ResultUtils.success(orderBusinessService.cancelPay(database, String.valueOf(deliverOrderNo)));
    }

    private BaseResponse<?> getPayOrderMessage(Map<String, Object> body, String database) {
        Object deliverOrderNo = body.get("orderNo");

        if (ObjectUtils.isEmpty(deliverOrderNo)) {

            deliverOrderNo = body.get("paymentOrderId");
        }

        if (ObjectUtils.isEmpty(deliverOrderNo)) {

            deliverOrderNo = body.get("orderId");
        }




        return ResultUtils.success(orderBusinessService.getOrder(database, String.valueOf(deliverOrderNo)));

    }

    private BaseResponse<?> getUniqueOrderNo(Map<String, Object> body, String database) {
        String productId = String.valueOf(body.get("productId"));
        String userId = String.valueOf(body.get("userId"));

        return ResultUtils.success(orderBusinessService.getUniqueOrderNo(productId,
                userId));
    }

    // 创建订单
    private BaseResponse<?> createOrder(Map<String, Object> body, String database) {
        OrderCreatRequest createRequest = objectMapper.convertValue(body, OrderCreatRequest.class);

        final LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());

        final PayLoginVo payLoginVo = new PayLoginVo();



        final String relevanceId = loginUser.getRelevanceId();
        final String relevanceTable = loginUser.getRelevanceTable();
        final String appId = loginUser.getAppId();


        final LambdaQueryWrapper<Login> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Login::getRelevanceId, relevanceId);
        queryWrapper.eq(Login::getRelevanceTable, CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, relevanceTable));
        Page<Login> loginPage = new Page<>(1,1);
        loginService.page(appId, loginPage, queryWrapper);
        Login login = loginPage.getRecords() == null || loginPage.getRecords().isEmpty() ? null : loginPage.getRecords().get(0);

        if (login == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "user does not exist");
        }

        payLoginVo.setLoginId(login.getLoginId());
        payLoginVo.setWxOpenId(login.getWxOpenId());
        payLoginVo.setPhoneNumber(login.getPhoneNumber());
        payLoginVo.setPassword(login.getPassword());
        payLoginVo.setUserName(login.getUserName());
        payLoginVo.setRelevanceId(login.getRelevanceId());
        payLoginVo.setRelevanceTable(login.getRelevanceTable());


        return ResultUtils.success(orderBusinessService.createPaymentOrder(database, createRequest, payLoginVo));
    }

//

    // 发货
    private BaseResponse<?> deliverOrder(Map<String, Object> body, String database) {
        Object deliverOrderNo = body.get("orderNo");

        if (ObjectUtils.isEmpty(deliverOrderNo)) {

            deliverOrderNo = body.get("paymentOrderId");
        }


        orderBusinessService.deliverOrder(database, String.valueOf(deliverOrderNo));
        return ResultUtils.success(true);
    }

    // 确认收货
    private BaseResponse<?> confirmOrder(Map<String, Object> body, String database) {
        Object deliverOrderNo = body.get("orderNo");
        if (ObjectUtils.isEmpty(deliverOrderNo)) {
            deliverOrderNo = body.get("paymentOrderId");
        }
        orderBusinessService.confirmReceipt(database, String.valueOf(deliverOrderNo));
        return ResultUtils.success(true);
    }

    /**
     * 获取支付参数
     *
     * @param body
     * @param database
     * @return
     */
    private BaseResponse<?> getPaymentParam(Map<String, Object> body, String database) {
        Object deliverOrderNo = body.get("orderNo");
        if (ObjectUtils.isEmpty(deliverOrderNo)) {
            deliverOrderNo = body.get("paymentOrderId");
        }
        return ResultUtils.success(orderBusinessService.getPaymentParam(database, String.valueOf(deliverOrderNo)));
    }

    /**
     * 处理回调
     *
     * @param payChannel
     * @param requestData
     * @param
     * @param headers
     * @param database
     * @return
     */
    public Object handleOrderCallback(String payChannel, String requestData, Map<String, String> headers, String database) {
        PayCallbackRequest payCallbackRequest = orderBusinessService.decryption(database, payChannel, requestData, headers);
        log.info("解密后的支付参数{} ,渠道{}", payCallbackRequest, payChannel);
        if (ObjectUtils.isEmpty(payCallbackRequest)) {
            return null;
        }
        return orderBusinessService.paySuccessCallback(database, payCallbackRequest);

    }
}
