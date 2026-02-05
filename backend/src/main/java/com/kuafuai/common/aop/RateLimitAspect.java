package com.kuafuai.common.aop;

import com.kuafuai.common.annotation.RateLimit;
import com.kuafuai.common.exception.RateLimitException;
import com.kuafuai.common.service.LocalAppRateLimitService;
import com.kuafuai.common.util.ServletUtils;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 限流切面
 * 拦截带有 @RateLimit 注解的方法，执行限流检查
 */
@Aspect
@Component
@Order(1) // 优先级高，尽早执行限流检查
@Slf4j
public class RateLimitAspect {

    @Autowired
    private LocalAppRateLimitService rateLimitService;

    /**
     * 环绕通知：拦截所有带 @RateLimit 注解的方法
     *
     * @param joinPoint 切点
     * @param rateLimit 限流注解实例
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {

        // 根据注解配置的维度类型，构建限流键
        String appId = null;
        String clientIp = null;
        RateLimit.DimensionType dimension = rateLimit.dimension();

        switch (dimension) {
            case APP_IP:
                appId = GlobalAppIdFilter.getAppId();
                clientIp = ServletUtils.getClientIp();
                break;
            case APP:
                appId = GlobalAppIdFilter.getAppId();
                clientIp = "ALL"; // 所有IP共享同一个限流器
                break;
            case IP:
                appId = "ALL"; // 所有应用共享同一个限流器
                clientIp = ServletUtils.getClientIp();
                break;
        }


        // 执行限流检查
        boolean allowed = rateLimitService.tryAcquire(appId, clientIp);
        log.info("限流检查 - 应用: {}, IP: {}, 维度: {} 结果: {}", appId, clientIp, dimension, allowed);

        if (!allowed) {
            // 限流触发
            throw new RateLimitException();
        }

        // 限流检查通过，执行目标方法
        return joinPoint.proceed();
    }
}
