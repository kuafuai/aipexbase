package com.kuafuai.common.entity;

import com.google.common.util.concurrent.RateLimiter;
import lombok.Data;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 应用限流统计信息 - 混合限流实现
 * 秒级：使用 Guava RateLimiter（令牌桶算法）
 * 分钟级：使用滑动窗口计数器
 */
@Data
public class AppRateLimitInfo {

    /**
     * 应用ID
     */
    private String appId;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 秒级限流器 (每秒20个请求) - 令牌桶算法
     */
    private final RateLimiter secondRateLimiter;

    /**
     * 分钟级请求时间戳队列 - 滑动窗口
     */
    private final ConcurrentLinkedDeque<Long> minuteRequestTimestamps;

    /**
     * 每分钟允许的最大请求数
     */
    private final int maxRequestsPerMinute;

    /**
     * 上次尝试获取许可的时间戳（毫秒）
     */
    private volatile long lastAttemptTime;

    public AppRateLimitInfo(String appId, String ipAddress, double permitsPerSecond, double permitsPerMinute) {
        this.appId = appId;
        this.ipAddress = ipAddress;
        // 秒级限流：使用令牌桶算法
        this.secondRateLimiter = RateLimiter.create(permitsPerSecond);
        // 分钟级限流：使用滑动窗口计数器
        this.minuteRequestTimestamps = new ConcurrentLinkedDeque<>();
        this.maxRequestsPerMinute = (int) permitsPerMinute;
        this.lastAttemptTime = System.currentTimeMillis();
    }

    /**
     * 尝试获取限流许可
     *
     * @return true表示允许请求，false表示被限流
     */
    public boolean tryAcquire() {
        this.lastAttemptTime = System.currentTimeMillis();
        long now = this.lastAttemptTime;

        // 1. 检查秒级限流（令牌桶）
        boolean secondAllowed = secondRateLimiter.tryAcquire();
        if (!secondAllowed) {
            return false;
        }

        // 2. 检查分钟级限流（滑动窗口）
        // 清理60秒前的旧请求记录
        long oneMinuteAgo = now - 60_000;
        while (!minuteRequestTimestamps.isEmpty() &&
                minuteRequestTimestamps.peekFirst() < oneMinuteAgo) {
            minuteRequestTimestamps.pollFirst();
        }

        // 检查当前窗口内的请求数
        int currentMinuteRequests = minuteRequestTimestamps.size();
        if (currentMinuteRequests >= maxRequestsPerMinute) {
            return false;
        }

        return minuteRequestTimestamps.offerLast(now);
    }

    /**
     * 检查是否能获取到令牌（不实际消耗令牌，仅查询状态）
     * 只检查分钟级限流，避免消耗秒级令牌
     *
     * @return true表示允许请求，false表示当前处于限流状态
     */
    public boolean canAcquire() {
        long now = System.currentTimeMillis();

        // 检查分钟级限流（滑动窗口）
        // 清理60秒前的旧请求记录
        long oneMinuteAgo = now - 60_000;
        while (!minuteRequestTimestamps.isEmpty() &&
                minuteRequestTimestamps.peekFirst() < oneMinuteAgo) {
            minuteRequestTimestamps.pollFirst();
        }

        // 检查当前窗口内的请求数
        int currentMinuteRequests = minuteRequestTimestamps.size();
        return currentMinuteRequests < maxRequestsPerMinute;
    }
}