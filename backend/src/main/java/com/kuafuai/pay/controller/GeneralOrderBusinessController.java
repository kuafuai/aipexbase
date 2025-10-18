package com.kuafuai.pay.controller;

import com.google.common.collect.Lists;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.dynamic_config.ConfigContext;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.pay.business.OrderFacadeService;
import com.kuafuai.pay.config.WxV3PayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/generalOrder")
@Slf4j
public class GeneralOrderBusinessController {


    @Resource
    private OrderFacadeService orderFacadeService;





    /**
     * 订单处理的统一入口
     * @param
     * @return
     */
    @PostMapping("/{operateName}")

    public BaseResponse<?> handleOrder(@PathVariable String operateName, @RequestBody
                                       Map<String, Object> body) {

        return orderFacadeService.handleOrder(GlobalAppIdFilter.getAppId(),operateName, body);

    }

    @GetMapping("/payMethod")

    public BaseResponse<?> payMethod() {
        final WxV3PayConfig wxV3PayConfig = WxV3PayConfig.builder()
                .appId(ConfigContext.getDatabase())
                .build();
        final ArrayList<String> strings = Lists.newArrayList();
        final Boolean wxEnable = wxV3PayConfig.getWxEnable();
        final Boolean mockEnable = wxV3PayConfig.getMockEnable();

        if (wxEnable) {
            strings.add("wechat");
        }

        if (mockEnable) {
            strings.add("mock");
        }

        return ResultUtils.success(strings);

    }


    @PostMapping("/callback/{database}/{payChannel}")
    public Object paySuccessCallback(@PathVariable String database,@PathVariable String payChannel,@RequestBody String requestData, @RequestHeader Map<String, String> headers) {
        try {
            ConfigContext.setDatabase(database);
            log.info("paySuccessCallback payChannel:{},requestData:{},headers:{}", payChannel, requestData, headers);
            return orderFacadeService.handleOrderCallback(payChannel,requestData,headers,database);
        }finally {
            ConfigContext.clear();
        }
    }
}
