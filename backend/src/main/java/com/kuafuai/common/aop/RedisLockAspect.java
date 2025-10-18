package com.kuafuai.common.aop;

import com.kuafuai.common.annotation.DistributedLock;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.ELUtils;
import com.kuafuai.common.util.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "cache.type", havingValue = "redis")
@Order(0) // 确保是最先执行的
public class RedisLockAspect {

    @Autowired
    private RedissonClient redissonClient;


    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // 获取 SpEL 表达式的 key 值
        // 解析 prefix 和 key
        String prefix = ELUtils.parseExpression(distributedLock.prefix(), joinPoint);
        String key = ELUtils.parseExpression(distributedLock.key(), joinPoint);
        String lockKey = prefix + key; // 组合成完整的锁 key
        long leaseTime = distributedLock.leaseTime();
        TimeUnit timeUnit = distributedLock.timeUnit();
        RLock lock = redissonClient.getLock(lockKey);

        log.info("start lock {}", lockKey);
        try {
            // 尝试获取锁
            if (lock.tryLock(distributedLock.waitTime(), leaseTime, timeUnit)) {
                // 获取到锁，执行方法
                log.info("process lock func {}", lockKey);
                final Object proceed = joinPoint.proceed();
                return proceed;
            } else {
//                log.info("Unable to acquire lock for key: {}", lockKey);
                // 未获取到锁，抛出异常或自定义处理逻辑
                throw new BusinessException(ErrorCode.NOT_GET_LOCK, "Unable to acquire lock for key: " + lockKey);
            }
        } catch (BusinessException e) {
//            if (e instanceof BusinessException) {
            BusinessException businessException = e;
            final int code = businessException.getCode();
            if (ErrorCode.NOT_GET_LOCK.getCode() == code) {
                log.info("Unable to acquire lock for key: {}", lockKey);
                final ServletRequestAttributes requestAttributes = ServletUtils.getRequestAttributes();
                if (requestAttributes != null) {
                    log.info("web env Unable to acquire lock for key: {}", lockKey);
//                      说明是springweb请求中的环境正常抛出异常就行
                    throw e;
                }
                log.info("not web env Unable to acquire lock for key: {}", lockKey);
                return null;
            }

            throw e;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                log.info("end lock {}", lockKey);
                // 只有是当前线程才去释放锁
                lock.unlock();
            }
        }
    }


}
