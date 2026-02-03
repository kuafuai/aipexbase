package com.kuafuai.pay.controller;

import com.google.common.collect.Lists;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.pay.business.OrderFacadeService;
import com.kuafuai.pay.config.WxV3PayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/generalOrder")
@Slf4j
public class GeneralOrderBusinessController {

    @Resource
    private OrderFacadeService orderFacadeService;

    /**
     * 生成唯一支付单id
     * 创建订单
     * 轮训支付单状态
     */
    @PostMapping("/{operateName}")
    public BaseResponse<?> handleOrder(@PathVariable String operateName, @RequestBody Map<String, Object> body) {
        final String appId = GlobalAppIdFilter.getAppId();
        return executeWithDataSource(appId,() ->
                orderFacadeService.handleOrder(GlobalAppIdFilter.getAppId(), operateName, body)
        );

    }

    @GetMapping("/payMethod")

    public BaseResponse<?> payMethod() {
        final String appId = ConfigContext.getDatabase();
        return executeWithDataSource(appId,()->{
            final WxV3PayConfig wxV3PayConfig = WxV3PayConfig.builder()
                    .appId(appId)
                    .build();
            final ArrayList<String> strings = Lists.newArrayList();
            final Boolean wxEnable = wxV3PayConfig.getWxEnable();
            final Boolean mockEnable = wxV3PayConfig.getMockEnable();
            final Boolean stripeEnable = wxV3PayConfig.getStripeEnable();

            if (wxEnable) {
                strings.add("wechat");
            }

            if (mockEnable) {
                strings.add("mock");
            }

            if (stripeEnable) {
                strings.add("stripe");
            }

            return ResultUtils.success(strings);
        });
    }


    @PostMapping("/callback/{database}/{payChannel}")
    public Object paySuccessCallback(@PathVariable String database, @PathVariable String payChannel, @RequestBody String requestData, @RequestHeader Map<String, String> headers) {
        return executeWithDataSource(database,()->{
            try {
                ConfigContext.setDatabase(database);
                log.info("paySuccessCallback payChannel:{},requestData:{},headers:{}", payChannel, requestData, headers);
                return orderFacadeService.handleOrderCallback(payChannel, requestData, headers, database);
            } finally {
                ConfigContext.clear();
            }
        });
    }



    private  <T> T executeWithDataSource(String appId,Supplier<T> supplier) {

        final String rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(appId, "app");

        try {
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);
            return supplier.get();
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    private void executeWithDataSource(String appId,Runnable runnable) {

        final String rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(appId, "app");

        try {
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);
            runnable.run();
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }
}
