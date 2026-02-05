package com.kuafuai.dynamic.controller;


import com.google.common.collect.Maps;
import com.kuafuai.common.constant.HttpStatus;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.service.LocalAppRateLimitService;
import com.kuafuai.common.util.I18nUtils;
import com.kuafuai.common.util.ServletUtils;
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
    
    @Autowired
    private LocalAppRateLimitService rateLimitService;

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
        // 获取应用ID和客户端IP并执行限流检查
        String appId = GlobalAppIdFilter.getAppId();
        String clientIp = ServletUtils.getClientIp();
        log.info("收到请求 - 应用ID: {}, IP: {}, 表名: {}, 方法: {}", appId, clientIp, table, method);
        
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("应用ID为空，拒绝请求");
            throw new BusinessException("error.app.id.required", "应用ID不能为空");
        }
        
        if (clientIp == null || clientIp.trim().isEmpty()) {
            log.warn("无法获取客户端IP，拒绝请求");
            throw new BusinessException("error.client.ip.required", "无法识别客户端IP");
        }
        
        // 执行限流检查（基于应用ID和IP地址）
        boolean allowed = rateLimitService.tryAcquire(appId, clientIp);
        log.info("限流检查结果 - 应用ID: {}, IP: {}, 允许: {}", appId, clientIp, allowed);
        
        if (!allowed) {
            // 限流触发，返回详细的限流信息
            LocalAppRateLimitService.RateLimitStatus status = rateLimitService.getStatus(appId, clientIp);
            String message = I18nUtils.getOrDefault("error.rate_limit.too_many_requests", 
                "请求过于频繁，请稍后再试");
            message += String.format(" (当前: %d/%d 请求/秒, %d/%d 请求/分钟)", 
                status.getCurrentSecondRequests(), 20,
                status.getCurrentMinuteRequests(), 50);
            
            log.warn("应用 {} IP {} 触发限流: 秒级 {}/{}, 分钟级 {}/{}", 
                appId, clientIp,
                status.getCurrentSecondRequests(), 20,
                status.getCurrentMinuteRequests(), 50);
            
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
        
        // 限流检查通过，执行正常业务逻辑
        TriFunction<String, String, Map<String, Object>, Object> function = methodHandlers.get(method.toLowerCase());
        if (function == null) {
            throw new BusinessException("error.data.method.not_found");
        }

        return function.apply(appId, table, data);
    }

    @PostMapping("/export")
    public void exportExcel(
            @RequestParam("table") String table,
            @RequestBody Map<String, Object> data) {
        String database = GlobalAppIdFilter.getAppId();
        dynamicService.export(database, table, data);
    }
}