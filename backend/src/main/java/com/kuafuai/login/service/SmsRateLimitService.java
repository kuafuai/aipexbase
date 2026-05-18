package com.kuafuai.login.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kuafuai.login.config.SmsDefaultProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 平台默认 SMS 渠道的 IP 限流：固定窗口 1 分钟 / 1 小时。
 * 限流命中由调用方决定降级或拒绝。
 */
@Slf4j
@Service
public class SmsRateLimitService {

    @Autowired
    private SmsDefaultProperties smsDefaults;

    private Cache<String, AtomicInteger> minuteBuckets;
    private Cache<String, AtomicInteger> hourBuckets;

    @PostConstruct
    public void init() {
        minuteBuckets = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(100_000)
                .build();
        hourBuckets = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100_000)
                .build();
    }

    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        int perMinute = smsDefaults.getRateLimit().getPerMinute();
        int perHour = smsDefaults.getRateLimit().getPerHour();
        try {
            AtomicInteger m = minuteBuckets.get(ip, AtomicInteger::new);
            if (m.incrementAndGet() > perMinute) {
                m.decrementAndGet();
                log.warn("SMS 每分钟限流命中, ip={}, perMinute={}", ip, perMinute);
                return false;
            }
            AtomicInteger h = hourBuckets.get(ip, AtomicInteger::new);
            if (h.incrementAndGet() > perHour) {
                h.decrementAndGet();
                m.decrementAndGet();
                log.warn("SMS 每小时限流命中, ip={}, perHour={}", ip, perHour);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("SMS 限流检查异常, ip={}", ip, e);
            return true;
        }
    }
}
