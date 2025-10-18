package com.kuafuai.common.aop;

import com.kuafuai.common.annotation.DistributedLock;
import com.kuafuai.common.util.ELUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "cache.type", havingValue = "local")
public class LocalLockAspect {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 解析 prefix 和 key
        String prefix = ELUtils.parseExpression(distributedLock.prefix(), joinPoint);
        String key = ELUtils.parseExpression(distributedLock.key(), joinPoint);
        String lockKey = prefix + key; // 组合成完整的锁 key

        // 获取 leaseTime
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();

        // 从 map 中获取锁
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean acquired = false;

        log.info("Attempting to lock {}", lockKey);
        try {
            // 尝试获取锁，设置等待时间
            acquired = lock.tryLock(leaseTime, timeUnit);
            if (acquired) {
                log.info("Lock acquired: {}", lockKey);
                // 获取到锁，执行方法
                return joinPoint.proceed();
            } else {
                log.warn("Unable to acquire lock for key: {}", lockKey);
                throw new RuntimeException("Unable to acquire lock for key: " + lockKey);
            }
        } finally {
            if (acquired) {
                lock.unlock();
                log.info("Lock released: {}", lockKey);
                // 锁释放后，移除锁对象，避免内存泄漏
                lockMap.remove(lockKey);
            }
        }
    }


}
