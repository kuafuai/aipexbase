package com.kuafuai.usage.vo;

import lombok.Data;

@Data
public class UsageSummaryVO {
    /** 24 小时内 API 调用次数 (含 4xx/5xx). */
    private Long apiCalls24h;

    /** 7 天内 API 调用次数. */
    private Long apiCalls7d;

    /** 24 小时内错误率 = err/total, 保留 4 位小数. */
    private Double errorRate24h;

    /** 24 小时内平均响应耗时, 毫秒. */
    private Long avgLatencyMs24h;

    /** 项目累计注册用户数 (login 表 count). */
    private Long userCount;

    /** 数据库中所有业务表的行数汇总 (来自 information_schema, 是估算值). */
    private Long dbRows;

    /** 数据库占用 MB (data_length + index_length). */
    private Double dbSizeMb;

    /** 项目累计上传文件数. */
    private Long fileCount;

    /** 项目累计上传文件总大小 MB. */
    private Double fileStorageMb;
}
