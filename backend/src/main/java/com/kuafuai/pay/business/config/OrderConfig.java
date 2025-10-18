package com.kuafuai.pay.business.config;


import com.google.common.collect.Maps;
import com.kuafuai.common.dynamic_config.service.DynamicConfigBusinessService;
import com.kuafuai.common.util.SpringUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

//@Configuration
//@DBConfiguration
@Data
public class OrderConfig {
    /**
     * 订单未支付的过期时间，单位分钟
     */
//    @Value("${order.expire.time:1}")
    private Integer orderExpireTime = 1;

    /**
     * 订单自动收货时间，单位天
     */
//    @Value("${order.automatic.receiptTime:3}")
    private Integer automaticReceiptTime = 3;


//    @Value("${order.automatic.schedule.corn:0 0 3 * * ?}")

    private String automaticScheduleCorn = "0 0 3 * * ?";

    @Value("${order.orderStatusField:-1}")
    private String orderStatusField;

    /**
     * 退款状态字段
     */
    @Value("${order.refundStatusValue:-1}")
    private String refundStatusValue;


    public static OrderConfig.Builder builder() {
        return new OrderConfig.Builder();
    }


    public static class Builder {

        private Map<String, Object> map;


        private String appId;

        private final Map<String, String> parseMap = Maps.newHashMap();

        public OrderConfig.Builder map(Map<String, String> map) {
            if (map != null && !map.isEmpty()) {
                map.forEach(parseMap::putIfAbsent);
            }
            return this;
        }

        public OrderConfig.Builder appId(String appId) {
            DynamicConfigBusinessService dynamicConfigBusinessService = SpringUtils.getBean(DynamicConfigBusinessService.class);
            Map<String, String> map = dynamicConfigBusinessService.getSystemConfig(appId);
            map.forEach(parseMap::putIfAbsent);
            return this;
        }





        public OrderConfig build() {
            OrderConfig config = new OrderConfig();

            config.orderExpireTime = Integer.valueOf(parseMap.getOrDefault("order.expire.time", "1"));
            config.automaticReceiptTime = Integer.valueOf(parseMap.getOrDefault("order.automatic.receiptTime", "3"));
            config.automaticScheduleCorn = parseMap.getOrDefault("wx.pay.app-id", "0");
            config.orderStatusField = parseMap.getOrDefault("order.orderStatusField", "0");
            config.refundStatusValue = parseMap.getOrDefault("order.refundStatusValue", "0");


            return config;
        }




    }
}
