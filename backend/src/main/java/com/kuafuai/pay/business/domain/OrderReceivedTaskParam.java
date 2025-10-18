package com.kuafuai.pay.business.domain;


import com.kuafuai.common.delay_task.domain.DelayTask;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 订单自动收货任务参数
 */
@Data
@AllArgsConstructor
public class OrderReceivedTaskParam extends DelayTask {

    /**
     * 业务系统订单号
     */
    private String orderNo;

}
