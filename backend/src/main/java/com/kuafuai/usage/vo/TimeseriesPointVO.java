package com.kuafuai.usage.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimeseriesPointVO {
    /** 时间桶字符串, e.g. "2026-07-17" 或 "2026-07-17 14:00" */
    private String bucket;
    private Long callCount;
    private Long errCount;
}
