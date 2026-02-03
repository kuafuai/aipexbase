package com.kuafuai.common.constant;

/**
 * 限流相关常量
 */
public class RateLimitConstants {
    
    /**
     * 限流配置常量
     */
    public static final class Config {
        /** 每秒请求数限制 */
        public static final int REQUESTS_PER_SECOND = 20;
        
        /** 每分钟请求数限制 */
        public static final int REQUESTS_PER_MINUTE = 50;
        
        /** 时间窗口大小（秒） */
        public static final int TIME_WINDOW_SECONDS = 60;
    }
    
    /**
     * 缓存键前缀
     */
    public static final class CacheKey {
        /** 应用限流统计键前缀 */
        public static final String APP_RATE_LIMIT_PREFIX = "rate_limit:app:";
    }
    
    /**
     * 响应消息
     */
    public static final class Message {
        /** 请求过于频繁 */
        public static final String TOO_MANY_REQUESTS = "error.rate_limit.too_many_requests";
        
        /** 限流配置错误 */
        public static final String CONFIG_ERROR = "error.rate_limit.config_error";
    }
}