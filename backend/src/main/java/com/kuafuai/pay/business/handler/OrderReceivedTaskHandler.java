package com.kuafuai.pay.business.handler;


import com.kuafuai.common.delay_task.handler.AbstractDelayedTaskHandler;
import com.kuafuai.pay.business.GeneralOrderBusinessService;
import com.kuafuai.pay.business.domain.OrderReceivedTaskParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@Slf4j
public class OrderReceivedTaskHandler extends AbstractDelayedTaskHandler<OrderReceivedTaskParam> {


    @Resource
    private GeneralOrderBusinessService generalOrderBusinessService;

    @Override
    public Class<?> getSupportedType() {
        return OrderReceivedTaskParam.class;
    }

    @Override
    public void handler(String database, OrderReceivedTaskParam param) {
        log.info("订单自动确认收货,订单号{}", param.getOrderNo());
//      关闭订单
        generalOrderBusinessService.confirmReceipt(database, param.getOrderNo());

        log.info("订单自动确认收货,订单号:{}", param.getOrderNo());
    }
}
