package com.kuafuai.usage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 调用量小时聚合. 由 UsageAggregator 定时 flush.
 *
 * 纯 append 模型: 每次 flush 追加行, 不做 upsert. 多实例部署时各写各的, 零锁冲突.
 * 读端通过 SUM(...) GROUP BY 聚合出总数.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("usage_hourly")
public class UsageHourly {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String appId;

    private LocalDateTime bucketHour;

    private String endpointGroup;

    private String statusBucket;

    private Long callCount;

    private Long latencySumMs;

    private LocalDateTime createdAt;
}
