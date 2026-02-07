package com.kuafuai.common.constant;

/**
 * 限流相关常量
 */
public class RateLimitConstants {

    /**
     * 限流配置常量
     */
    public static final class Config {
        /**
         * 每秒请求数限制
         */
        public static final double REQUESTS_PER_SECOND = 50.0;

        /**
         * 每分钟请求数限制
         */
        public static final double REQUESTS_PER_MINUTE = 150.0;

        /**
         * 数据清理间隔时间（秒）
         */
        public static final int CLEANUP_INTERVAL_SECONDS = 60;

        /**
         * 数据过期时间（秒）- 2分钟未使用则清理
         */
        public static final long EXPIRATION_TIME_SECONDS = 120;
    }
}