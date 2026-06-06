package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.PointsRecord;

import java.math.BigDecimal;
import java.util.List;

public interface PointsRecordService extends IService<PointsRecord> {

    /**
     * 查询用户积分变动记录（按时间倒序）
     *
     * @param codeFlyingUserId 码上飞用户ID
     * @param limit            最多返回条数
     * @return 记录列表
     */
    List<PointsRecord> getRecordsByUser(String codeFlyingUserId, int limit);

    /**
     * 保存积分记录到 DEFAULT 库（独立事务，强制走 DEFAULT 数据源）
     */
    void saveToDefault(String codeFlyingUserId, String type, String subType, BigDecimal amount);
}
