package com.kuafuai.dynamic.controller;


import com.google.common.collect.Maps;
import com.kuafuai.dynamic.service.DynamicStatisticsService;
import com.kuafuai.dynamic.service.TriFunction;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;


/**
 * 统一统计入口
 */
@RestController
@RequestMapping("/api/statistics")
@Slf4j
public class UnifiedStatisticsController {

    private final Map<String, TriFunction<String, String, Map<String, Object>, Object>> methodHandlers = Maps.newConcurrentMap();

    @Autowired
    private DynamicStatisticsService dynamicStatisticsService;

    @PostConstruct
    public void init() {
        log.info("========================初始化statistics=================");
        methodHandlers.put("count", dynamicStatisticsService::count);
    }

    @PostMapping("/invoke")
    public Object handle(
            @RequestParam("table") String table,
            @RequestParam(value = "method", defaultValue = "count") String method,
            @RequestBody Map<String, Object> data
    ) {
        String database = GlobalAppIdFilter.getAppId();
        TriFunction<String, String, Map<String, Object>, Object> function = methodHandlers.get(method.toLowerCase());
        if (function == null) {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }
        return function.apply(database, table, data);
    }

}
