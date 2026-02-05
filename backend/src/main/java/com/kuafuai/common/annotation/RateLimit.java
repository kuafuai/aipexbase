package com.kuafuai.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 应用于需要限流的方法上，基于应用ID和IP地址进行限流控制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流维度：APP_IP（应用+IP）、APP（仅应用）、IP（仅IP）
     */
    DimensionType dimension() default DimensionType.APP_IP;

    /**
     * 限流维度类型
     */
    enum DimensionType {
        /**
         * 基于应用ID + IP地址
         */
        APP_IP,
        /**
         * 仅基于应用ID
         */
        APP,
        /**
         * 仅基于IP地址
         */
        IP
    }
}
