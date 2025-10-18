package com.kuafuai.pay.db.service;

import com.kuafuai.common.db.DynamicDatabaseService;
import com.kuafuai.pay.db.domain.GeneralOrders;
import com.kuafuai.pay.enums.PayStatus;

/**
 * @author www.macpe.cn
 * @description 针对表【general_orders】的数据库操作Service
 * @createDate 2025-05-09 17:20:09
 */
public interface GeneralOrdersService extends  DynamicDatabaseService<GeneralOrders> {

    /**
     * 根据订单号获取订单信息
     *
     * @param orderNo
     * @return
     */
    GeneralOrders getByOrderNo(String database,String orderNo);


    /**
     * 根据支付订单号获取订单信息
     *
     * @param paymentOrderId
     * @return
     */
    GeneralOrders getByPaymentOrderId(String database,String paymentOrderId);

    /**
     * 根据退款
     */

    GeneralOrders getByRefundId(String database,String refundId);

    /**
     * 更新支付订单状态
     */

    void updatePaymentStatusByPaymentOrderId(String database,String paymentOrderId, PayStatus originalStatus, PayStatus newStatus);

    /**
     * 更新支付订单状态
     *
     * @param orderNo
     * @param originalStatus
     * @param newStatus
     */

    void updatePaymentStatusByOrderNo(String database,String orderNo, PayStatus originalStatus, PayStatus newStatus);


    GeneralOrders getByOpId(String database,String opId);
}
