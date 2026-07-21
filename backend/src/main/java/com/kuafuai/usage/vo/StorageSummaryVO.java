package com.kuafuai.usage.vo;

import lombok.Data;

@Data
public class StorageSummaryVO {
    private Long totalFiles;
    private Double totalMb;
    /** 最近 7 天上传文件数 */
    private Long uploadedThisWeek;
    /** 最近 30 天上传文件数 */
    private Long uploadedThisMonth;
}
