package com.kuafuai.common.service;

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
 * 基于内存实现，为每个APP_ID提供独立的限流控制
 */
@Slf4j
@Service
public class LocalAppRateLimitService {
    
    /** 应用限流信息缓存 - 使用 appId:ipAddress 作为键 */
    private final ConcurrentHashMap<String, AppRateLimitInfo> rateLimitCache = new ConcurrentHashMap<>();
    
    /** 定时清理过期数据的调度器 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public LocalAppRateLimitService() {
        // 启动定时清理任务，每分钟清理一次过期数据
        scheduler.scheduleAtFixedRate(this::cleanupExpiredData, 
            RateLimitConstants.Config.TIME_WINDOW_SECONDS, 
            RateLimitConstants.Config.TIME_WINDOW_SECONDS, 
            TimeUnit.SECONDS);
    }
    
    /**
     * 检查并更新应用的请求次数
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
        AppRateLimitInfo info = rateLimitCache.computeIfAbsent(cacheKey, k -> new AppRateLimitInfo(appId, ipAddress));
        log.info("应用 {} IP {} 限流检查 - 缓存大小: {}", appId, ipAddress, rateLimitCache.size());
        return checkAndIncrement(info);
    }
    
    /**
     * 检查并增加计数
     */
    private boolean checkAndIncrement(AppRateLimitInfo info) {
        long now = System.currentTimeMillis() / 1000;
        
        // 检查并重置秒计数器
        if (now - info.getCurrentSecondStart() >= 1) {
            info.resetSecondCounter();
            info.setCurrentSecondStart(now);
            log.debug("重置秒计数器: 应用={} 时间={}", info.getAppId(), now);
        }
        
        // 检查并重置分钟计数器
        if (now - info.getCurrentMinuteStart() >= RateLimitConstants.Config.TIME_WINDOW_SECONDS) {
            info.resetMinuteCounter();
            info.setCurrentMinuteStart(now);
            log.debug("重置分钟计数器: 应用={} 时间={}", info.getAppId(), now);
        }
        
        // 检查限流规则
        int currentSecondCount = info.getSecondCount();
        int currentMinuteCount = info.getMinuteCount();
        
        log.info("应用 {} 当前计数 - 秒级: {}/{}, 分钟级: {}/{}", 
            info.getAppId(), 
            currentSecondCount, RateLimitConstants.Config.REQUESTS_PER_SECOND,
            currentMinuteCount, RateLimitConstants.Config.REQUESTS_PER_MINUTE);
        
        if (currentSecondCount >= RateLimitConstants.Config.REQUESTS_PER_SECOND) {
            log.warn("应用 {} 秒级限流触发: {}/{}", 
                info.getAppId(), currentSecondCount, RateLimitConstants.Config.REQUESTS_PER_SECOND);
            return false;
        }
        
        if (currentMinuteCount >= RateLimitConstants.Config.REQUESTS_PER_MINUTE) {
            log.warn("应用 {} 分钟级限流触发: {}/{}", 
                info.getAppId(), currentMinuteCount, RateLimitConstants.Config.REQUESTS_PER_MINUTE);
            return false;
        }
        
        // 增加计数
        info.increment();
        log.debug("应用 {} 计数增加 - 秒级: {}, 分钟级: {}", 
            info.getAppId(), info.getSecondCount(), info.getMinuteCount());
        return true;
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
                RateLimitConstants.Config.REQUESTS_PER_SECOND, 
                RateLimitConstants.Config.REQUESTS_PER_MINUTE);
        }
        
        return new RateLimitStatus(
            info.getSecondCount(),
            info.getMinuteCount(),
            RateLimitConstants.Config.REQUESTS_PER_SECOND,
            RateLimitConstants.Config.REQUESTS_PER_MINUTE
        );
    }
    
    /**
     * 清理过期数据
     */
    private void cleanupExpiredData() {
        long now = System.currentTimeMillis() / 1000;
        long expirationTime = RateLimitConstants.Config.TIME_WINDOW_SECONDS * 2; // 2分钟未使用的数据
        
        rateLimitCache.entrySet().removeIf(entry -> {
            AppRateLimitInfo info = entry.getValue();
            long lastAccess = Math.max(info.getLastSecondResetTime(), info.getLastMinuteResetTime());
            boolean expired = (now - lastAccess) > expirationTime;
            
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