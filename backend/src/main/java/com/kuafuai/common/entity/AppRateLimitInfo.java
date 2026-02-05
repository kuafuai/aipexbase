package com.kuafuai.common.entity;

import lombok.Data;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 应用限流统计信息
 */
@Data
public class AppRateLimitInfo {
    
    /** 应用ID */
    private String appId;
    
    /** IP地址 */
    private String ipAddress;
    
    /** 秒级计数器 */
    private final AtomicInteger secondCounter = new AtomicInteger(0);
    
    /** 分钟级计数器 */
    private final AtomicInteger minuteCounter = new AtomicInteger(0);
    
    /** 上次重置秒计数器的时间戳（秒） */
    private volatile long lastSecondResetTime;
    
    /** 上次重置分钟计数器的时间戳（秒） */
    private volatile long lastMinuteResetTime;
    
    /** 当前秒的起始时间戳 */
    private volatile long currentSecondStart;
    
    /** 当前分钟的起始时间戳 */
    private volatile long currentMinuteStart;
    
    public AppRateLimitInfo(String appId, String ipAddress) {
        this.appId = appId;
        this.ipAddress = ipAddress;
        long now = System.currentTimeMillis() / 1000;
        this.lastSecondResetTime = now;
        this.lastMinuteResetTime = now;
        this.currentSecondStart = now;
        this.currentMinuteStart = now;
    }
    
    /**
     * 重置秒计数器
     */
    public void resetSecondCounter() {
        secondCounter.set(0);
        lastSecondResetTime = System.currentTimeMillis() / 1000;
    }
    
    /**
     * 重置分钟计数器
     */
    public void resetMinuteCounter() {
        minuteCounter.set(0);
        lastMinuteResetTime = System.currentTimeMillis() / 1000;
    }
    
    /**
     * 增加计数
     */
    public void increment() {
        secondCounter.incrementAndGet();
        minuteCounter.incrementAndGet();
    }
    
    /**
     * 获取当前秒计数
     */
    public int getSecondCount() {
        return secondCounter.get();
    }
    
    /**
     * 获取当前分钟计数
     */
    public int getMinuteCount() {
        return minuteCounter.get();
    }
}