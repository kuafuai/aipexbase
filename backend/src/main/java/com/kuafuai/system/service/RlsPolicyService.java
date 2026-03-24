package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.RlsPolicy;

import java.util.List;

/**
 * RLS 策略服务接口
 */
public interface RlsPolicyService extends IService<RlsPolicy> {

    /**
     * 从 CREATE POLICY SQL 语句创建策略
     *
     * @param appId    应用ID
     * @param sql      CREATE POLICY SQL 语句
     * @param priority 优先级（可选，默认 0）
     * @return 创建的策略
     */
    RlsPolicy createFromSql(String appId, String sql, Integer priority);


    /**
     * 根据表名获取所有策略
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @return 策略列表
     */
    List<RlsPolicy> getPoliciesByTable(String appId, String tableName);

    /**
     * 根据表名和操作类型获取策略
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @param operation 操作类型（SELECT/INSERT/UPDATE/DELETE/ALL）
     * @return 策略列表
     */
    List<RlsPolicy> getPoliciesByTableAndOperation(String appId, String tableName, String operation);

    /**
     * 启用/禁用策略
     *
     * @param policyId 策略ID
     * @param enabled  是否启用
     * @return 是否成功
     */
    boolean togglePolicy(Long policyId, Boolean enabled);

    /**
     * 删除表的所有策略
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @return 删除数量
     */
    int deleteByTable(String appId, String tableName);
}
