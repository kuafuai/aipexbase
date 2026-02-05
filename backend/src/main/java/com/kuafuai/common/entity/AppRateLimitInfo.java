package com.kuafuai.common.entity;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;

/**
 * 应用限流统计信息 - 基于Guava RateLimiter实现
 */
@Data
public class AppRateLimitInfo {
    
    /** 应用ID */
    private String appId;
    
    /** IP地址 */
    private String ipAddress;
    
    /** 秒级限流器 (每秒20个请求) */
    private final RateLimiter secondRateLimiter;
    
    /** 分钟级限流器 (每分钟50个请求) */
    private final RateLimiter minuteRateLimiter;
    
    /** 上次尝试获取许可的时间戳（毫秒）*/
    private volatile long lastAttemptTime;
    
    public AppRateLimitInfo(String appId, String ipAddress, double permitsPerSecond, double permitsPerMinute) {
        this.appId = appId;
        this.ipAddress = ipAddress;
        // 创建两个独立的RateLimiter实例
        this.secondRateLimiter = RateLimiter.create(permitsPerSecond);
        this.minuteRateLimiter = RateLimiter.create(permitsPerMinute / 60.0); // 转换为每秒的速率
        this.lastAttemptTime = System.currentTimeMillis();
    }
    
    /**
     * 尝试获取限流许可
     * @return true表示允许请求，false表示被限流
     */
    public boolean tryAcquire() {
        this.lastAttemptTime = System.currentTimeMillis();
        
        // 同时检查秒级和分钟级限流
        boolean secondAllowed = secondRateLimiter.tryAcquire();
        boolean minuteAllowed = minuteRateLimiter.tryAcquire();
        
        return secondAllowed && minuteAllowed;
    }
    
    /**
     * 获取当前估计的请求速率
     * 注意：RateLimiter不提供精确的当前请求数统计，这里返回配置的速率作为参考
     */
    public int getCurrentSecondEstimate() {
        // 返回配置的速率作为估计值
        return (int) Math.round(secondRateLimiter.getRate());
    }
    
    public int getCurrentMinuteEstimate() {
        // 返回配置的分钟速率作为估计值
        return (int) Math.round(minuteRateLimiter.getRate() * 60);
    }
}