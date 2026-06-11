package com.kuafuai.common.entity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 应用限流统计信息 - 混合限流实现
 * 秒级：使用 Guava RateLimiter（令牌桶算法）
 * 分钟级：使用滑动窗口计数器
 */
@Data
@Slf4j
public class AppRateLimitInfo {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * IP地址
     */
    private String ipAddress;

    private final int maxRequestsPerSecond;
    private final int maxRequestsPerMinute;
    private final ConcurrentLinkedDeque<Long> secondRequestTimestamps;
    private final ConcurrentLinkedDeque<Long> minuteRequestTimestamps;

    /**
     * 上次尝试获取许可的时间戳（毫秒）
     */
    private volatile long lastAttemptTime;

    public AppRateLimitInfo(String appId, String ipAddress, double permitsPerSecond, double permitsPerMinute) {
        this.appId = appId;
        this.ipAddress = ipAddress;
        this.maxRequestsPerSecond = (int) permitsPerSecond;
        this.maxRequestsPerMinute = (int) permitsPerMinute;
        this.secondRequestTimestamps = new ConcurrentLinkedDeque<>();
        this.minuteRequestTimestamps = new ConcurrentLinkedDeque<>();
        this.lastAttemptTime = System.currentTimeMillis();
        log.info("限流:{},{}====={},{}====", appId, ipAddress, permitsPerSecond, permitsPerMinute);
    }

    /**
     * 尝试获取限流许可
     *
     * @return true表示允许请求，false表示被限流
     */
    public synchronized boolean tryAcquire() {
        long now = System.currentTimeMillis();
        this.lastAttemptTime = now;

        // 1. 秒级滑动窗口
        long oneSecondAgo = now - 1000;
        while (!secondRequestTimestamps.isEmpty() && secondRequestTimestamps.peekFirst() < oneSecondAgo) {
            secondRequestTimestamps.pollFirst();
        }
        if (secondRequestTimestamps.size() >= maxRequestsPerSecond) {
            log.info("限流:{}:{}====秒级触发：{}", appId, ipAddress, secondRequestTimestamps.size());
            return false;
        }

        // 2. 分钟级滑动窗口
        long oneMinuteAgo = now - 60_000;
        while (!minuteRequestTimestamps.isEmpty() && minuteRequestTimestamps.peekFirst() < oneMinuteAgo) {
            minuteRequestTimestamps.pollFirst();
        }
        if (minuteRequestTimestamps.size() >= maxRequestsPerMinute) {
            log.info("限流:{}:{}====分钟级触发：{}", appId, ipAddress, minuteRequestTimestamps.size());
            return false;
        }

        secondRequestTimestamps.offerLast(now);
        minuteRequestTimestamps.offerLast(now);
        return true;
    }

    /**
     * 检查是否能获取到令牌（不实际消耗令牌，仅查询状态）
     * 只检查分钟级限流，避免消耗秒级令牌
     *
     * @return true表示允许请求，false表示当前处于限流状态
     */
    public synchronized boolean canAcquire() {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60_000;
        while (!minuteRequestTimestamps.isEmpty() && minuteRequestTimestamps.peekFirst() < oneMinuteAgo) {
            minuteRequestTimestamps.pollFirst();
        }
        return minuteRequestTimestamps.size() < maxRequestsPerMinute;
    }
}