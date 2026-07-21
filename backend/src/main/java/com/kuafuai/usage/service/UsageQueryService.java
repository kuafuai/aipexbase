package com.kuafuai.usage.service;

import com.kuafuai.config.db.DatabaseRouterAspect;
import com.kuafuai.config.db.DynamicDataSourceContextHolder;
import com.kuafuai.usage.mapper.FileUploadLogMapper;
import com.kuafuai.usage.mapper.TenantStatsMapper;
import com.kuafuai.usage.mapper.UsageHourlyMapper;
import com.kuafuai.usage.vo.EndpointCountVO;
import com.kuafuai.usage.vo.TimeseriesPointVO;
import com.kuafuai.usage.vo.UsageSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageQueryService {

    private final UsageHourlyMapper usageHourlyMapper;
    private final TenantStatsMapper tenantStatsMapper;
    private final FileUploadLogMapper fileUploadLogMapper;

    public UsageSummaryVO summary(String appId) {
        UsageSummaryVO vo = new UsageSummaryVO();

        // ---- 主库: usage_hourly ----
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        LocalDateTime h24 = now.minusHours(24);
        LocalDateTime d7 = now.minusDays(7);

        long calls24 = nz(usageHourlyMapper.sumCallCount(appId, h24, now));
        long calls7d = nz(usageHourlyMapper.sumCallCount(appId, d7, now));
        long errs24 = nz(usageHourlyMapper.sumErrCount(appId, h24, now));

        vo.setApiCalls24h(calls24);
        vo.setApiCalls7d(calls7d);
        vo.setErrorRate24h(calls24 == 0 ? 0d : round4((double) errs24 / calls24));
        vo.setAvgLatencyMs24h(calls24 == 0 ? 0L : nz(usageHourlyMapper.sumLatencyMs(appId, h24, now)) / calls24);

        // ---- 租户库: information_schema (schema 名 = appId) ----
        withTenant(appId, () -> {
            vo.setDbRows(nz(tenantStatsMapper.sumEstimatedRows(appId)));
            long bytes = nz(tenantStatsMapper.sumStorageBytes(appId));
            vo.setDbSizeMb(round4(bytes / 1024d / 1024d));
        });

        // ---- 主库: file_upload_log ----
        vo.setFileCount(nz(fileUploadLogMapper.countByApp(appId)));
        long fileBytes = nz(fileUploadLogMapper.sumBytesByApp(appId));
        vo.setFileStorageMb(round4(fileBytes / 1024d / 1024d));

        return vo;
    }

    public List<TimeseriesPointVO> timeseries(String appId, String range) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        switch (range) {
            case "24h":
                return usageHourlyMapper.timeseriesByHour(appId, now.minusHours(24), now);
            case "7d":
                return usageHourlyMapper.timeseriesByDay(appId, now.minusDays(7), now);
            case "30d":
                return usageHourlyMapper.timeseriesByDay(appId, now.minusDays(30), now);
            default:
                return Collections.emptyList();
        }
    }

    public List<EndpointCountVO> topEndpoints(String appId, String range, int limit) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        LocalDateTime from;
        switch (range) {
            case "24h":
                from = now.minusHours(24);
                break;
            case "7d":
                from = now.minusDays(7);
                break;
            case "30d":
                from = now.minusDays(30);
                break;
            default:
                return Collections.emptyList();
        }
        return usageHourlyMapper.topEndpoints(appId, from, now, limit);
    }

    private void withTenant(String appId, Runnable r) {
        try {
            String rdsKey = DatabaseRouterAspect.getOrAllocateRdsKey(appId, "app");
            DynamicDataSourceContextHolder.setDataSourceType(rdsKey);
            r.run();
        } catch (Exception e) {
            log.warn("withTenant query failed for {}: {}", appId, e.getMessage());
        } finally {
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    private static long nz(Long v) {
        return v == null ? 0L : v;
    }

    private static double round4(double v) {
        return Math.round(v * 10000d) / 10000d;
    }
}
