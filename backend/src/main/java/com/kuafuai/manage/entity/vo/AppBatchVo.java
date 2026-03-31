package com.kuafuai.manage.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量创建应用和表的请求实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppBatchVo {
    /**
     * 应用名称
     */
    private String name;

    /**
     * 应用所有者ID（外部用户ID）
     */
    private String userId;

    /**
     * 是否需要认证
     */
    private Boolean needAuth;

    /**
     * 认证表名
     */
    private String authTable;

    /**
     * 配置JSON
     */
    private String configJson;

    /**
     * 需要创建的表列表
     */
    private List<TableVo> tables;

    /**
     * RLS 策略 SQL 列表（CREATE POLICY 语句）
     */
    private List<String> rlsPolicies;
}
