package com.kuafuai.usage;

import com.kuafuai.usage.entity.UsageHourly;
import com.kuafuai.usage.mapper.UsageHourlyMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 请求 -> 内存桶累加, 每 60s flush 到主库 usage_hourly.
 *
 * 每条 HTTP 请求走 O(1) 的 map lookup + 两次 LongAdder.add, 不打 DB.
 * flush 时快照当前 map, 换一个新 map 继续接下来的写入; 快照按 appId 分组批量 upsert.
 *
 * 崩溃最多丢 60s 未 flush 的桶. 服务优雅关闭时会主动 flush 一次 (@PreDestroy).
 */
@Component
@Slf4j
public class UsageAggregator {

    /** flush 间隔, 单位 ms. */
    private static final long FLUSH_INTERVAL_MS = 60_000L;

    /** 单条 INSERT 最大行数, 超过就拆多条. */
    private static final int CHUNK_SIZE = 500;

    @Resource
    private UsageHourlyMapper usageHourlyMapper;

    /** 内存桶: key -> (次数 + 耗时累加). */
    private volatile ConcurrentHashMap<Key, Accumulator> buckets = new ConcurrentHashMap<>();

    /**
     * 采集入口, 供 UsageTrackingFilter 在 chain.doFilter 之后调用.
     */
    public void record(String appId, String endpointGroup, int statusCode, long latencyMs) {
        if (appId == null || endpointGroup == null || EndpointClassifier.isIgnored(endpointGroup)) {
            return;
        }
        Key k = new Key(appId, currentHour(), endpointGroup, statusBucketOf(statusCode));
        Accumulator acc = buckets.computeIfAbsent(k, x -> new Accumulator());
        acc.count.increment();
        acc.latencySumMs.add(latencyMs);
    }

    @Scheduled(fixedDelay = FLUSH_INTERVAL_MS)
    public void scheduledFlush() {
        try {
            flush();
        } catch (Exception e) {
            log.error("UsageAggregator flush failed", e);
        }
    }

    @PreDestroy
    public void onShutdown() {
        log.info("UsageAggregator shutdown, flushing pending buckets");
        try {
            flush();
        } catch (Exception e) {
            log.error("UsageAggregator shutdown flush failed", e);
        }
    }

    private void flush() {
        Map<Key, Accumulator> snapshot = drain();
        if (snapshot.isEmpty()) {
            return;
        }

        List<UsageHourly> rows = new ArrayList<>(snapshot.size());
        snapshot.forEach((k, acc) -> rows.add(UsageHourly.builder()
                .appId(k.appId)
                .bucketHour(k.bucketHour)
                .endpointGroup(k.endpointGroup)
                .statusBucket(k.statusBucket)
                .callCount(acc.count.sum())
                .latencySumMs(acc.latencySumMs.sum())
                .build()));

        // 纯 append: 多实例并发写零冲突, 读端 SUM 聚合. 走主库 (@Scheduled 线程没 TL, 天然 DEFAULT).
        // 分 chunk 避免单条 INSERT 语句过大.
        int total = 0;
        for (int i = 0; i < rows.size(); i += CHUNK_SIZE) {
            List<UsageHourly> chunk = rows.subList(i, Math.min(i + CHUNK_SIZE, rows.size()));
            usageHourlyMapper.batchInsert(chunk);
            total += chunk.size();
        }
        log.info("UsageAggregator flushed {} rows", total);
    }

    /** 换一个新 map, 返回旧的. 期间新写入落到新 map. */
    private Map<Key, Accumulator> drain() {
        ConcurrentHashMap<Key, Accumulator> old = this.buckets;
        this.buckets = new ConcurrentHashMap<>();
        return old;
    }

    private static LocalDateTime currentHour() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
    }

    private static String statusBucketOf(int status) {
        if (status >= 500) return "5xx";
        if (status >= 400) return "4xx";
        if (status >= 300) return "3xx";
        return "2xx";
    }

    private static final class Key {
        final String appId;
        final LocalDateTime bucketHour;
        final String endpointGroup;
        final String statusBucket;

        Key(String appId, LocalDateTime bucketHour, String endpointGroup, String statusBucket) {
            this.appId = appId;
            this.bucketHour = bucketHour;
            this.endpointGroup = endpointGroup;
            this.statusBucket = statusBucket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key k = (Key) o;
            return Objects.equals(appId, k.appId)
                    && Objects.equals(bucketHour, k.bucketHour)
                    && Objects.equals(endpointGroup, k.endpointGroup)
                    && Objects.equals(statusBucket, k.statusBucket);
        }

        @Override
        public int hashCode() {
            return Objects.hash(appId, bucketHour, endpointGroup, statusBucket);
        }
    }

    private static final class Accumulator {
        final LongAdder count = new LongAdder();
        final LongAdder latencySumMs = new LongAdder();
    }
}
