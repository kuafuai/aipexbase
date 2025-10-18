package com.kuafuai.pay.business.domain;


import com.kuafuai.common.delay_task.domain.DelayTask;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderExpireTaskParam extends DelayTask {

    /**
     * 业务系统订单号
     */
    private String orderNo;

}
