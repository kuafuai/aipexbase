package com.kuafuai.dynamic.controller;


import com.google.common.collect.Maps;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.dynamic.service.DynamicService;
import com.kuafuai.dynamic.service.TriFunction;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@Slf4j
public class UnifiedDataController {

    private final Map<String, TriFunction<String, String, Map<String, Object>, Object>> methodHandlers = Maps.newConcurrentMap();

    @Autowired
    private DynamicService dynamicService;

    @PostConstruct
    public void init() {
        log.info("========================初始化database=================");
        methodHandlers.put("add", dynamicService::add);
        methodHandlers.put("delete", dynamicService::delete);
        methodHandlers.put("update", dynamicService::update);
        methodHandlers.put("get", dynamicService::get);
        methodHandlers.put("list", dynamicService::list);
        methodHandlers.put("page", dynamicService::page);
        methodHandlers.put("deletebatch", dynamicService::deleteBatch);
    }


    @PostMapping("/invoke")
    public Object handle(
            @RequestParam("table") String table,
            @RequestParam(value = "method", defaultValue = "save") String method,
            @RequestBody Map<String, Object> data
    ) {
        String database = GlobalAppIdFilter.getAppId();
        TriFunction<String, String, Map<String, Object>, Object> function = methodHandlers.get(method.toLowerCase());
        if (function == null) {
            throw new BusinessException("Unsupported method: " + method);
        }

        return function.apply(database, table, data);
    }

    @PostMapping("/export")
    public void exportExcel(
            @RequestParam("table") String table,
            @RequestBody Map<String, Object> data) {
        String database = GlobalAppIdFilter.getAppId();
        dynamicService.export(database, table, data);
    }
}
