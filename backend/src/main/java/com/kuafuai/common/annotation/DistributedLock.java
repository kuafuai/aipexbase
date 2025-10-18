package com.kuafuai.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    String prefix() default ""; // 使用 SpEL 表达式动态生成前缀

    String key();               // 使用 SpEL 表达式从方法参数中获取

    long leaseTime() default 10; // 锁的持有时间

    TimeUnit timeUnit() default TimeUnit.SECONDS; // 持有时间单位

    long waitTime() default 0;// 锁的等待时间
}
