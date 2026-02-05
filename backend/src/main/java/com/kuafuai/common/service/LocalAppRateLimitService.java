package com.kuafuai.common.service;

import com.google.common.util.concurrent.RateLimiter;
import com.kuafuai.common.constant.RateLimitConstants;
import com.kuafuai.common.entity.AppRateLimitInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 本地应用限流服务
 * 基于Guava RateLimiter实现，为每个APP_ID提供独立的限流控制
 */
@Slf4j
@Service
public class LocalAppRateLimitService {
    
    /** 应用限流信息缓存 - 使用 appId:ipAddress 作为键 */
    private final ConcurrentHashMap<String, AppRateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    /** 定时清理过期数据的调度器 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public LocalAppRateLimitService() {
        // 启动定时清理任务，定期清理过期数据
        scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 
            RateLimitConstants.Config.CLEANUP_INTERVAL_SECONDS, 
            RateLimitConstants.Config.CLEANUP_INTERVAL_SECONDS, 
            TimeUnit.SECONDS);
    }
    
    /**
     * 检查并尝试获取限流许可
     * @param appId 应用ID
     * @param ipAddress IP地址
     * @return true表示允许请求，false表示被限流
     */
    public boolean tryAcquire(String appId, String ipAddress) {
        if (appId == null || appId.trim().isEmpty()) {
            log.warn("无效的应用ID: {}", appId);
            return false;
        }
        
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            log.warn("无效的IP地址: {}", ipAddress);
            return false;
        }
        
        // 使用 appId:ipAddress 作为唯一键
        String cacheKey = appId + ":" + ipAddress;
        AppRateLimitInfo info = rateLimitCache.computeIfAbsent(cacheKey, 
            k -> new AppRateLimitInfo(appId, ipAddress, 
                RateLimitConstants.Config.REQUESTS_PER_SECOND,
                RateLimitConstants.Config.REQUESTS_PER_MINUTE));
        
        boolean allowed = info.tryAcquire();
        log.info("应用 {} IP {} 限流检查 - 结果: {}, 缓存大小: {}", 
            appId, ipAddress, allowed, rateLimitCache.size());
        
        return allowed;
    }
    
    /**
     * 获取应用当前的限流状态
     * @param appId 应用ID
     * @param ipAddress IP地址
     */
    public RateLimitStatus getStatus(String appId, String ipAddress) {
        String cacheKey = appId + ":" + ipAddress;
        AppRateLimitInfo info = rateLimitCache.get(cacheKey);
        if (info == null) {
            return new RateLimitStatus(0, 0, 
                (int) RateLimitConstants.Config.REQUESTS_PER_SECOND, 
                (int) RateLimitConstants.Config.REQUESTS_PER_MINUTE);
        }
        
        return new RateLimitStatus(
            info.getCurrentSecondEstimate(),
            info.getCurrentMinuteEstimate(),
            (int) RateLimitConstants.Config.REQUESTS_PER_SECOND,
            (int) RateLimitConstants.Config.REQUESTS_PER_MINUTE
        );
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        long now = System.currentTimeMillis();
        
        rateLimitCache.entrySet().removeIf(entry -> {
            AppRateLimitInfo info = entry.getValue();
            long timeSinceLastAttempt = now - info.getLastAttemptTime();
            boolean expired = timeSinceLastAttempt > 
                TimeUnit.SECONDS.toMillis(RateLimitConstants.Config.EXPIRATION_TIME_SECONDS);
            
            if (expired) {
                log.debug("清理过期的应用限流数据: {}", entry.getKey());
            }
            
            return expired;
        });
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 限流状态信息
     */
    public static class RateLimitStatus {
        private final int currentSecondRequests;
        private final int currentMinuteRequests;
        private final int maxSecondRequests;
        private final int maxMinuteRequests;
        
        public RateLimitStatus(int currentSecondRequests, int currentMinuteRequests, 
                             int maxSecondRequests, int maxMinuteRequests) {
            this.currentSecondRequests = currentSecondRequests;
            this.currentMinuteRequests = currentMinuteRequests;
            this.maxSecondRequests = maxSecondRequests;
            this.maxMinuteRequests = maxMinuteRequests;
        }
        
        public boolean isLimited() {
            return currentSecondRequests >= maxSecondRequests || 
                   currentMinuteRequests >= maxMinuteRequests;
        }
        
        // getters
        public int getCurrentSecondRequests() { return currentSecondRequests; }
        public int getCurrentMinuteRequests() { return currentMinuteRequests; }
        public int getMaxSecondRequests() { return maxSecondRequests; }
        public int getMaxMinuteRequests() { return maxMinuteRequests; }
    }
}