package com.kuafuai.pay.business;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.kuafuai.common.annotation.DistributedLock;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.delay_task.DelayTaskScheduler;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.common.util.SnowflakeIdGenerator;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.common.util.UUID;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.service.TokenService;
import com.kuafuai.pay.business.config.OrderConfig;
import com.kuafuai.pay.business.constant.OrderTypeConstant;
import com.kuafuai.pay.business.domain.OrderCreatRequest;
import com.kuafuai.pay.business.domain.OrderExpireTaskParam;
import com.kuafuai.pay.business.domain.OrderReceivedTaskParam;
import com.kuafuai.pay.db.domain.GeneralOrders;
import com.kuafuai.pay.db.service.GeneralOrdersService;
import com.kuafuai.pay.domain.PayCallbackRequest;
import com.kuafuai.pay.domain.PayLoginVo;
import com.kuafuai.pay.domain.PaymentOrderDetail;
import com.kuafuai.pay.enums.PayStatus;
import com.kuafuai.pay.factoary.PayFactory;
import com.kuafuai.pay.service.PayService;
import com.kuafuai.pay.stats.PayStatusStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class GeneralOrderBusinessService {

    @Resource
    private GeneralOrdersService generalOrdersService;


    @Resource
    private PayFactory payFactory;

    @Resource
    private Cache cache;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SnowflakeIdGenerator snowflakeIdGenerator;


    @Resource
    private DelayTaskScheduler delayTaskScheduler;

//    @Resource
//    private OrderConfig orderConfig;


    @Autowired(required = false)
    private TokenService tokenService;


    /**
     * 获取唯一订单的操作ID，防重校验,5分钟有效
     *
     * @return
     */
    @DistributedLock(prefix = "payOpLock:", key = "#productId+':'+#userId")
    public String getUniqueOrderNo(String productId, String userId) {

        String preKey = "payOp:" + productId + ":" + userId;
        final Object object = cache.getCacheObject(preKey);

        String opId = null;

        if (ObjectUtils.isNotEmpty(object)) {
            opId = String.valueOf(object);
        }

//      如果没有就写入
        if (StringUtils.isEmpty(opId)) {
            final String uuid = UUID.randomUUID().toString();
            cache.setCacheObject(preKey, uuid);
            cache.expire(preKey, 5, TimeUnit.MINUTES);
            opId = uuid;
        }

        return opId;
    }


    public GeneralOrders getOrder(String database, String orderNo) {

        GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);
        if (byOrderNo == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
            return byOrderNo;
        }
        final String paymentChannel = byOrderNo.getPaymentChannel();
        final PayService payService = payFactory.getPayService(database, paymentChannel);
        if (byOrderNo.getOrderStatus().equals(PayStatus.UNPAID)) {
            final PaymentOrderDetail paymentOrderDetail = payService.getPaymentOrderDetail(byOrderNo.getOrderNo());
            if (paymentOrderDetail != null && PayStatus.PAID_SUCCESS.equals(paymentOrderDetail.getStatus())) {
                //        更新订单状态是已支付
                final PayCallbackRequest payCallbackRequest = new PayCallbackRequest();
                payCallbackRequest.setPaymentOrderId(paymentOrderDetail.getPaymentOrderId());
                payCallbackRequest.setOrderNo(byOrderNo.getOrderNo());
                payCallbackRequest.setPaymentTime(paymentOrderDetail.getPayTime());
                payCallbackRequest.setPaymentAmount(paymentOrderDetail.getAmount());
                payCallbackRequest.setPayStatus(paymentOrderDetail.getStatus());
                payCallbackRequest.setExtraParamMap(new HashMap<>());
                paySuccessCallback(database, payCallbackRequest);
                byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);
            }
        }
        return byOrderNo;
    }


    /**
     * 创建订单
     *
     * @param orderCreatRequest
     * @return
     */
    @DistributedLock(prefix = "orderLock:", key = "#orderCreatRequest.opId")
    public Object createPaymentOrder(String database, OrderCreatRequest orderCreatRequest, PayLoginVo payLoginVo) {

        final Integer quality = orderCreatRequest.getQuality();
        final BigDecimal totalPrice = orderCreatRequest.getTotalPrice();
        final BigDecimal priceSingle = orderCreatRequest.getPriceSingle();
        final Integer productId = orderCreatRequest.getProductId();
        final Integer userId = orderCreatRequest.getUserId();
        final String payChanel = orderCreatRequest.getPayChanel();
        final String opId = orderCreatRequest.getOpId();
        final String productSubject = orderCreatRequest.getProductSubject();
        String remark = orderCreatRequest.getRemark();
        String preKey = "payOp:" + productId + ":" + userId;

        final LoginUser loginUser = tokenService.getLoginUser(ServletUtils.getRequest());
        final Long loginUserUserId = loginUser.getUserId();
//          查询login表信息
//        final Login byId = loginService.getById(loginUserUserId);


        try {
            final Object cacheObject = cache.getCacheObject(preKey);

            if (ObjectUtils.isEmpty(cacheObject)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "opId is not exist");
            }


            PayService payService = payFactory.getPayService(database, payChanel);

            //      查询是否该操作订单的id

            GeneralOrders gOrders = generalOrdersService.getByOpId(database, opId);


            if (ObjectUtils.isNotEmpty(gOrders)) {


//              判断用户是否支付
                return payService.getPaymentParam(gOrders.getPaymentOrderId(), null);
            }


//      获取订单号，保存订单

            String orderNo = orderCreatRequest.getOrderNo();
//          判断该订单号是否存在

            boolean isHaveOrderNo;
            if (StringUtils.isEmpty(orderCreatRequest.getOrderNo())) {
                orderNo = String.valueOf(snowflakeIdGenerator.nextId());
                isHaveOrderNo = false;
            } else {
//              如果该订单号存在，那么直接返回支付参数进行支付，不去创建新的订单
                final GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);
                if (byOrderNo != null) {
                    final PayStatus payStatus = byOrderNo.getPayStatus();
                    if (payStatus == null || payStatus.equals(PayStatus.UNPAID)) {
//                      应该判断是否已过期
                        final boolean b = expireOrder(database, orderNo);
                        if (!b) {
                            return payService.getPaymentParam(byOrderNo.getPaymentOrderId(),
                                    null);
                        } else {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单已过期,请刷新页面后重试");
                        }


                    } else {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单已付款,请勿重复支付");
                    }
                }
                isHaveOrderNo = true;
            }


            BigDecimal total;
            if (ObjectUtils.isNotEmpty(totalPrice)) {
                total = totalPrice;
            } else {
                total = priceSingle.multiply(new BigDecimal(quality));
            }


            final String paymentOrder = payService.createPaymentOrder(payLoginVo, orderNo, total, productSubject, null, database);

            if (StringUtils.isBlank(orderCreatRequest.getTel())) {
//                throw new BusinessException("联系人手机号不能为空");
                orderCreatRequest.setTel("unknow");
            }
//        if (StringUtils.isBlank(orderCreatRequest.getRecipient())) {
//            throw new BusinessException("收件人姓名不能为空");
//        }
            if (StringUtils.isBlank(orderCreatRequest.getShippingAddress())) {
                orderCreatRequest.setShippingAddress("unknow");
//                throw new BusinessException("收货地址不能为空");
            }

            String finalOrderNo = orderNo;
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    final GeneralOrders generalOrders = new GeneralOrders();
                    generalOrders.setOrderNo(finalOrderNo);
                    generalOrders.setUserId(userId);
                    generalOrders.setOpId(opId);
                    generalOrders.setOrderType(OrderTypeConstant.NORMAL);
                    generalOrders.setOrderStatus(PayStatus.UNPAID);
                    generalOrders.setPayStatus(PayStatus.UNPAID);
                    generalOrders.setProductsId(productId);
                    generalOrders.setQuality(quality == null ? 1 : quality);
                    generalOrders.setTotalAmount(total);
                    generalOrders.setDiscountAmount(BigDecimal.ZERO);
                    generalOrders.setShippingFee(BigDecimal.ZERO);
                    generalOrders.setActualAmount(total);
                    generalOrders.setPaymentChannel(payChanel);
                    generalOrders.setCreateTime(LocalDateTime.now());
                    generalOrders.setUpdateTime(LocalDateTime.now());
                    generalOrders.setRemark(remark);
                    generalOrders.setTel(orderCreatRequest.getTel());
                    generalOrders.setRecipient(orderCreatRequest.getRecipient());
                    generalOrders.setShippingAddress(orderCreatRequest.getShippingAddress());
                    generalOrders.setPaymentOrderId(paymentOrder);
                    generalOrders.setTableName(orderCreatRequest.getTableName());
                    generalOrders.setFieldName(orderCreatRequest.getFieldName());

                    final boolean save = generalOrdersService.save(database, generalOrders);

//                  更新订单的数据
                    if (!save) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "init order fail");
                    }

                    if (isHaveOrderNo) {
                        updatePaymentOrdersIdToBusinessOrder(orderCreatRequest.getTableName(), orderCreatRequest.getFieldName(), finalOrderNo,
                                String.valueOf(generalOrders.getId()));
                    }

                } catch (Exception e) {
                    status.setRollbackOnly();
                    try {
                        throw e;
                    } catch (ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

//      设置订单过期逻辑
            final OrderConfig orderConfig = OrderConfig.builder().appId(ConfigContext.getDatabase()).build();
            final Integer orderExpireTime = orderConfig.getOrderExpireTime();
//      不需要判空，如果是空，那么springboot 就无法启动
            if (orderExpireTime > 0) {
                delayTaskScheduler.schedule(GlobalAppIdFilter.getAppId(), new OrderExpireTaskParam(orderNo),
                        orderExpireTime * 60, TimeUnit.SECONDS);
            }
            return payService.getPaymentParam(paymentOrder, null);
        } finally {
            cache.deleteObject(preKey);
        }

    }

    /**
     * 更新订单号到业务订单号
     *
     * @param
     * @param finalOrderNo
     * @param
     * @throws ClassNotFoundException
     */
    public void updatePaymentOrdersIdToBusinessOrder(String tableName, String fieldName, String finalOrderNo, String paymentOrderId) throws ClassNotFoundException {


//        final String pascalCase = toPascalCase(tableName);
//        final String primaryName = getPrimaryName(pascalCase);
//        if (primaryName == null) {
//            throw new BusinessException("订单表主键字段不存在");
//        }
//        IService bean = SpringUtils.getBean(pascalCase);
//
//        final UpdateWrapper<?> updateWrapper = new UpdateWrapper();
//        updateWrapper.eq(primaryName, finalOrderNo).set(fieldName
//                , paymentOrderId);
//        final boolean update = bean.update(updateWrapper);
//        if (!update) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "init order fail");
//        }
    }

    @Nullable
    private static String getPrimaryName(String pascalCase) throws ClassNotFoundException {
        String packageName = "com.kuafuai.web.entity." + pascalCase;
        final Class<?> aClass = Class.forName(packageName);
        final Field[] fields = aClass.getDeclaredFields();
        String primaryName = null; // 主键字段
        for (Field field : fields) {
            if (field.isAnnotationPresent(TableId.class)) {
                final TableId annotation = field.getAnnotation(TableId.class);

                primaryName = annotation.value();
                break;
            }
        }
        return primaryName;
    }


    /**
     * 获取支付参数
     *
     * @param orderNo
     * @return
     */
    public Object getPaymentParam(String database, String orderNo) {
        GeneralOrders generalOrders = generalOrdersService.getByOrderNo(database, orderNo);

        final String paymentOrderId = generalOrders.getPaymentOrderId();

        final String paymentChannel = generalOrders.getPaymentChannel();
        PayService payService = payFactory.getPayService(database, paymentChannel);
        return payService.getPaymentParam(paymentOrderId, null);
    }

    /**
     * 订单支付成功回调
     */
    @DistributedLock(prefix = "orderLock:", key = "#payCallbackRequest.paymentOrderId")
    public Object paySuccessCallback(String database, PayCallbackRequest payCallbackRequest) {
        String paymentOrderId = payCallbackRequest.getPaymentOrderId();
//        String orderNo = payCallbackRequest.getOrderNo();
        LocalDateTime paymentTime = payCallbackRequest.getPaymentTime();
        BigDecimal paymentAmount = payCallbackRequest.getPaymentAmount();
//        Map<String, Object> extraParamMap = payCallbackRequest.getExtraParamMap();

        if (ObjectUtils.isEmpty(paymentTime)) {
            paymentTime = LocalDateTime.now();
        }

//      更新订单状态为已支付
        PayService payService = null;
        try {
            GeneralOrders byPaymentOrderId = generalOrdersService
                    .getByPaymentOrderId(database, payCallbackRequest.getOrderNo());
            if (ObjectUtils.isEmpty(byPaymentOrderId)) {
                byPaymentOrderId = generalOrdersService.getByOrderNo(database, payCallbackRequest.getPaymentOrderId());
            }

            if (ObjectUtils.isEmpty(byPaymentOrderId)) {
                log.info("订单支付成功回调，订单不存在, {}", paymentOrderId);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单不存在");
            }

            final String paymentChannel = byPaymentOrderId.getPaymentChannel();

            if (paymentAmount == null) {
                paymentAmount = byPaymentOrderId.getTotalAmount();
                // 如果支付金额是空，那么就获取实际的支付金额
            }

            payService = payFactory.getPayService(database, paymentChannel);

            //          说明已经更新过了
            if (PayStatus.PAID_SUCCESS.equals(byPaymentOrderId.getPayStatus())) {
                return "success";
            }


            BigDecimal finalPaymentAmount = paymentAmount;
            LocalDateTime finalPaymentTime = paymentTime;
            GeneralOrders finalByPaymentOrderId = byPaymentOrderId;
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    boolean update = generalOrdersService.update(database, new UpdateWrapper<GeneralOrders>()
                            .eq("order_no", finalByPaymentOrderId.getOrderNo())
                            .set("pay_status", PayStatus.PAID_SUCCESS)
                            .set("payment_time", finalPaymentTime.format(dateTimeFormatter))
                            .set("order_status", PayStatus.PAID_SUCCESS)
                            .set("payment_order_id", payCallbackRequest.getPaymentOrderId())
                            .set("payment_amount", finalPaymentAmount) // 更新支付金额是总金额
                    );
//                  更新业务订单表中的订单号

                    if (update) {
                        final String tableName = finalByPaymentOrderId.getTableName();
                        final String fieldName = finalByPaymentOrderId.getFieldName();
                        if (StringUtils.isNotEmpty(tableName) && StringUtils.isNotEmpty(fieldName)) {
                            updatePaymentOrdersIdToBusinessOrder(tableName, fieldName,
                                    finalByPaymentOrderId.getOrderNo(),
                                    String.valueOf(finalByPaymentOrderId.getId()));
                        }
                    }
                } catch (Exception e) {
                    status.setRollbackOnly();
                    try {
                        throw e;
                    } catch (ClassNotFoundException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            return payService.payCallbackProcessSuccess();


        } catch (Exception e) {
//          如果获取到了pay_service 那么返回失败，等待支付回调方重试  ，否则直接抛出异常
            if (ObjectUtils.isNotEmpty(payService)) {
                log.error("订单支付成功回调异常, paymentOrderId {}", paymentOrderId, e);
                return payService.payCallbackProcessFail();
            }
            throw e;
        }
    }


    /**
     * 订单过期
     *
     * @param orderNo
     */
    @DistributedLock(prefix = "orderLock:", key = "#orderNo")
    public boolean expireOrder(String database, String orderNo) {
        if (ObjectUtils.isEmpty(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单号不能为空");
        }
        final GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);


        if (ObjectUtils.isEmpty(byOrderNo)) {
            log.error("订单不存在, {}", orderNo);
            return false;
        }

        if (!PayStatus.UNPAID.equals(byOrderNo.getPayStatus())) {
            log.info("订单状态不是待支付，不能关闭订单, orderNo {}", orderNo);
            return false;
        }

//      判断是否超过了订单过期时间

        final LocalDateTime createTime = byOrderNo.getCreateTime();
        final OrderConfig orderConfig = OrderConfig.builder().appId(ConfigContext.getDatabase()).build();
        if (!LocalDateTime.now().isAfter(createTime.plusMinutes(orderConfig.getOrderExpireTime()))) {
//          没有过期，等待主动过期
            return false;
        }

//      关闭订单
        final String paymentChannel = byOrderNo.getPaymentChannel();

        final PayService payService = payFactory.getPayService(database, paymentChannel);


        boolean b = payService.closePaymentOrder(byOrderNo.getPaymentOrderId(), byOrderNo.getOrderNo());

        if (b) {
//           关单成功，说明未支付，更新订单的状态是已过期
            final LambdaUpdateWrapper<GeneralOrders> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(GeneralOrders::getOrderNo, orderNo)
                    .set(GeneralOrders::getOrderStatus, PayStatus.TIMEOUT)
                    .set(GeneralOrders::getCancelTime, LocalDateTime.now())
                    .set(GeneralOrders::getCompleteTime, LocalDateTime.now());
            final boolean update = generalOrdersService.update(database, updateWrapper);
            if (!update) {
                log.error("订单关闭失败, {}", orderNo);
                return false;
            }
        }
        return true;

    }


    /**
     * 订单发货
     */

    @DistributedLock(prefix = "orderLock:", key = "#orderNo")
    public void deliverOrder(String database, String orderNo) {
        if (ObjectUtils.isEmpty(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单号不能为空");
        }

//     进行发货，状态变更

        final GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);

        if (ObjectUtils.isEmpty(byOrderNo)) {
            log.error("订单不存在, {}", orderNo);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
        }

        if (PayStatusStateMachine.canTransition(byOrderNo.getPayStatus(), PayStatus.PENDING_RECEIPT)) {
            log.info("订单状态不是已支付，不能发货, orderNo {}", orderNo);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单状态不是已支付，不能发货");
        }

//        支付成功 -> 待收货
        final LambdaUpdateWrapper<GeneralOrders> updateWrapper = new LambdaUpdateWrapper<>();
        final GeneralOrders generalOrders = new GeneralOrders();
        generalOrders.setDeliverTime(LocalDateTime.now());
        generalOrders.setOrderStatus(PayStatus.PENDING_RECEIPT);
        updateWrapper.and(wrapper -> {
                    wrapper.eq(GeneralOrders::getOrderNo, orderNo)
                            .or()
                            .eq(GeneralOrders::getPaymentOrderId, orderNo);
                })
                .eq(GeneralOrders::getOrderStatus, PayStatus.PAID_SUCCESS);
//                .set(GeneralOrders::getOrderStatus, PayStatus.PENDING_RECEIPT)
//                .set(GeneralOrders::getDeliverTime, LocalDateTime.now());

        final boolean update = generalOrdersService.update(database, generalOrders, updateWrapper);

        final OrderConfig orderConfig = OrderConfig.builder().appId(ConfigContext.getDatabase()).build();
//      提交确认收货的延迟任务
        final Integer automaticReceiptTime = orderConfig.getAutomaticReceiptTime();
        delayTaskScheduler.schedule(database, new OrderReceivedTaskParam(orderNo),
                automaticReceiptTime, TimeUnit.DAYS);

        if (!update) {
            log.error("订单发货失败, {}", orderNo);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "订单发货失败");
        }

    }


    /**
     * 确认收货
     *
     * @param orderNo
     */

    @DistributedLock(prefix = "orderLock:", key = "#orderNo")
    public void confirmReceipt(String database, String orderNo) {
        if (ObjectUtils.isEmpty(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单号不能为空");
        }

        final GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);

        if (ObjectUtils.isEmpty(byOrderNo)) {
            log.error("订单不存在, {}", orderNo);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }

        if (!PayStatusStateMachine.canTransition(byOrderNo.getOrderStatus(), PayStatus.CONFIRM)) {
            log.info("订单状态不是待收货，不能确认收货, orderNo {}", orderNo);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单当前不可确认收货");
        }

        final GeneralOrders generalOrders = new GeneralOrders();
        generalOrders.setCompleteTime(LocalDateTime.now());
        generalOrders.setOrderStatus(PayStatus.CONFIRM);
        LambdaUpdateWrapper<GeneralOrders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.and(wrapper -> {
                    wrapper.eq(GeneralOrders::getOrderNo, orderNo)
                            .or()
                            .eq(GeneralOrders::getPaymentOrderId, orderNo);
                })
                .eq(GeneralOrders::getOrderStatus, PayStatus.PENDING_RECEIPT);
//                .set(GeneralOrders::getOrderStatus, PayStatus.CONFIRM)
//                .set(GeneralOrders::getCompleteTime, LocalDateTime.now()); // 如果你有交付时间字段

        boolean updated = generalOrdersService.update(database, generalOrders, updateWrapper);

        if (!updated) {
            log.error("确认收货失败, orderNo {}", orderNo);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "确认收货失败");
        }

        log.info("订单已确认收货, orderNo {}", orderNo);
    }


    public PayCallbackRequest decryption(String database, String payChannel, String requestData, Map<String, String> headers) {

        final PayService payService = payFactory.getPayService(database, payChannel);


        return payService.callbackDecryption(requestData, headers);

    }

    public static String toPascalCase(String snake) {
        if (snake == null || snake.isEmpty()) {
            return "";
        }
        String[] parts = snake.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }

    @DistributedLock(prefix = "orderLock:", key = "#orderNo")
    public Boolean cancelPay(String database, String orderNo) {
        final GeneralOrders byOrderNo = generalOrdersService.getByOrderNo(database, orderNo);

        if (ObjectUtils.isEmpty(byOrderNo)) {
            log.error("订单不存在, {}", orderNo);
            return true;
        }
        final PayStatus orderStatus = byOrderNo.getOrderStatus();
        if (PayStatusStateMachine.canTransition(orderStatus, PayStatus.CANCELLED)) {
//
            final String paymentChannel = byOrderNo.getPaymentChannel();
            final PayService payService = payFactory.getPayService(database, paymentChannel);
            final boolean b = payService.closePaymentOrder(byOrderNo.getPaymentOrderId(), byOrderNo.getOrderNo());
            if (!b) {
                log.error("取消订单失败, {}", orderNo);
//              判断是否是支付成功
                final PaymentOrderDetail paymentOrderDetail = payService.getPaymentOrderDetail(byOrderNo.getOrderNo());
//              如果没有这个订单详情，直接返回，取消支付
                if (ObjectUtils.isEmpty(paymentOrderDetail)) {
                    log.error("取消支付，订单不存在, {}", orderNo);
                    return true;
                }
                final PayStatus payStatus = paymentOrderDetail.getStatus();
                if (payStatus == PayStatus.PAID_SUCCESS) {
//                  更新订单状态是已支付
                    final PayCallbackRequest payCallbackRequest = new PayCallbackRequest();
                    payCallbackRequest.setPaymentOrderId(byOrderNo.getPaymentOrderId());
                    payCallbackRequest.setOrderNo(byOrderNo.getOrderNo());
                    payCallbackRequest.setPaymentTime(paymentOrderDetail.getPayTime());
                    payCallbackRequest.setPaymentAmount(paymentOrderDetail.getAmount());
                    payCallbackRequest.setPayStatus(payStatus);
                    payCallbackRequest.setExtraParamMap(new HashMap<>());
                    paySuccessCallback(database, payCallbackRequest);
                    return true;
                } else {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "取消订单失败,请确认该订单是否已经支付");
                }

            }
//          修改订单状态是取消
            final LambdaUpdateWrapper<GeneralOrders> updateWrapper = new LambdaUpdateWrapper<>();
            GeneralOrders order = new GeneralOrders();
            order.setOrderStatus(PayStatus.CANCELLED);
            order.setCancelTime(LocalDateTime.now());  // 这里会走 TypeHandler
            order.setCompleteTime(LocalDateTime.now());
            updateWrapper.and(wrapper -> {
                        wrapper.eq(GeneralOrders::getOrderNo, orderNo)
                                .or()
                                .eq(GeneralOrders::getPaymentOrderId, orderNo);
                    })
                    .eq(GeneralOrders::getOrderStatus, orderStatus);
//                    .set(GeneralOrders::getOrderStatus, PayStatus.CANCELLED)
//                    .set(GeneralOrders::getCancelTime, LocalDateTime.now())
//                    .set(GeneralOrders::getCompleteTime, LocalDateTime.now());
            final boolean update = generalOrdersService.update(database, order, updateWrapper);
            if (!update) {
                log.error("取消订单失败, {}", orderNo);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "取消订单失败,请重试");
            }
            return true;
        } else {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单当前不可取消");
        }
    }
    @DistributedLock(prefix = "orderLock:refund:", key = "#id")
    public Boolean refund(String database,String id) {
        final GeneralOrders byId = generalOrdersService.getById(database,id);

        if (ObjectUtils.isEmpty(byId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单不存在");
        }

        final PayStatus orderStatus = byId.getPayStatus();
        if (!orderStatus.equals(PayStatus.PAID_SUCCESS)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该订单状态异常无法退款");
        }

//      查询订单状态

        final PayService payService = payFactory.getPayService(database,byId.getPaymentChannel());

        final PayStatus refundStatus =
                byId.getRefundStatus();
        if (PayStatus.REFUNDED.equals(refundStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该订单已退款");
        }

//
//        执行退款
        final long l = snowflakeIdGenerator.nextId();
        String refundNo = "refund_" + l;
        final String reason = "用户申请退款";
        final String refundId = payService.applyRefund(refundNo, byId.getPaymentOrderId(), byId.getActualAmount(),
                reason, byId.getActualAmount()); // 微信支付系统内的退款单号


        final GeneralOrders generalOrders = new GeneralOrders();
        generalOrders.setId(Integer.valueOf(id));
        generalOrders.setRefundId(refundId);
        generalOrders.setRefundNo(refundNo);
        generalOrders.setRefundReason(reason);
        generalOrders.setRefundAmount(byId.getActualAmount());
        generalOrders.setOrderStatus(PayStatus.REFUNDED);
        generalOrders.setRefundStatus(PayStatus.REFUNDED);


        final boolean update = generalOrdersService.updateById(database,generalOrders);
        if (!update) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退款信息保存失败");
        }


//        try {
//            String tableName = byId.getTableName();
//            if (StringUtils.isNotEmpty(tableName)) {
//                String camelCase = StrUtil.toCamelCase(tableName);
//                tableName = StrUtil.upperFirst(camelCase);
//            }
//            final IService service = (IService) SpringUtils.getBean(tableName);
//
//            final UpdateWrapper<?> updateWrapper = new UpdateWrapper();
//            final OrderConfig orderConfig = OrderConfig.builder().appId(ConfigContext.getDatabase()).build();
//            updateWrapper.eq(byId.getFieldName(), byId.getId())
//                    .set(orderConfig.getOrderStatusField()
//                            , orderConfig.getRefundStatusValue());
//            final boolean result = service.update(updateWrapper);
//            if (!result) {
//                log.error("更新业务订单状态失败,{} {}", id, byId);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return true;

    }
}
