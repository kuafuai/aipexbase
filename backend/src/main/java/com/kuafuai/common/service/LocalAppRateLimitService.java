package com.kuafuai.common.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kuafuai.common.constant.RateLimitConstants;
import com.kuafuai.common.entity.AppRateLimitInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

/**
 * 本地应用限流服务
 * 基于 Guava RateLimiter 和 Guava Cache 实现，为每个 APP_ID:IP 提供独立的限流控制
 */
@Slf4j
@Service
public class LocalAppRateLimitService {

    /**
     * 应用限流信息缓存 - 使用 Guava Cache 自动管理过期
     * 键格式: appId:ipAddress
     * 过期策略: 2分钟未访问自动清理
     */
    private final Cache<String, AppRateLimitInfo> rateLimitCache;

    public LocalAppRateLimitService() {
        this.rateLimitCache = CacheBuilder.newBuilder()
                // 基于访问时间的过期策略：2分钟未使用则过期
                .expireAfterAccess(RateLimitConstants.Config.EXPIRATION_TIME_SECONDS, TimeUnit.SECONDS)
                // 初始容量
                .initialCapacity(100)
                // 最大容量：防止内存溢出
                .maximumSize(10000)
                // 启用统计信息（可选，用于监控）
                .recordStats()
                // 移除监听器：记录清理日志
                .removalListener(notification -> {
                    log.info("清理限流缓存 - 键: {}, 原因: {}", notification.getKey(), notification.getCause());
                })
                .build();

        log.info("初始化限流服务 - 过期时间: {}秒, 最大容量: 10000", RateLimitConstants.Config.EXPIRATION_TIME_SECONDS);
    }

    /**
     * 检查并尝试获取限流许可
     *
     * @param appId     应用ID
     * @param ipAddress IP地址
     * @return true表示允许请求，false表示被限流
     */
    public boolean tryAcquire(String appId, String ipAddress) {
        // 使用 appId:ipAddress 作为唯一键
        String cacheKey = buildCacheKey(appId, ipAddress);

        try {
            // 从缓存获取或创建限流信息
            AppRateLimitInfo info = rateLimitCache.get(cacheKey,
                    () -> new AppRateLimitInfo(appId, ipAddress, RateLimitConstants.Config.REQUESTS_PER_SECOND, RateLimitConstants.Config.REQUESTS_PER_MINUTE));

            return info.tryAcquire();
        } catch (Exception e) {
            log.error("限流检查异常 - 应用: {}, IP: {}", appId, ipAddress, e);
            return true;
        }
    }

    /**
     * 检查是否处于限流状态（不消耗令牌，仅查询）
     *
     * @param appId     应用ID
     * @param ipAddress IP地址
     * @return true表示允许请求，false表示当前处于限流状态
     */
    public boolean isAllowed(String appId, String ipAddress) {
        String cacheKey = buildCacheKey(appId, ipAddress);

        try {
            // 检查缓存中是否存在该限流信息
            AppRateLimitInfo info = rateLimitCache.getIfPresent(cacheKey);
            if (info == null) {
                // 没有限流记录，允许通过
                return true;
            }

            // 检查是否能获取到令牌（不实际消耗）
            return info.canAcquire();
        } catch (Exception e) {
            log.error("限流状态查询异常 - 应用: {}, IP: {}", appId, ipAddress, e);
            return true;
        }
    }


    /**
     * 构建缓存键
     */
    private String buildCacheKey(String appId, String ipAddress) {
        return appId + ":" + ipAddress;
    }

    /**
     * 服务关闭时清理缓存
     */
    @PreDestroy
    public void shutdown() {
        log.info("关闭限流服务，清理缓存 - 当前大小: {}", rateLimitCache.size());
        rateLimitCache.invalidateAll();
        rateLimitCache.cleanUp();
    }
}
