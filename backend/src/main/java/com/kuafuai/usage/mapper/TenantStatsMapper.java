package com.kuafuai.usage.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 面向租户库的辅助查询. 调用前需要通过 DynamicDataSourceContextHolder 切到对应租户 rds
 * (确定连的是哪个物理 MySQL 实例); appId 作为 schema 名传进来做 WHERE 过滤.
 */
@Mapper
public interface TenantStatsMapper {

    /**
     * 租户 schema 下所有业务表的估算行数.
     * 注意 information_schema.tables.TABLE_ROWS 对 InnoDB 是估算值, 展示够用.
     */
    @Select("SELECT COALESCE(SUM(TABLE_ROWS), 0) " +
            "FROM information_schema.tables " +
            "WHERE TABLE_SCHEMA = #{appId}")
    Long sumEstimatedRows(@Param("appId") String appId);

    /**
     * 租户 schema 数据+索引占用字节.
     */
    @Select("SELECT COALESCE(SUM(DATA_LENGTH + INDEX_LENGTH), 0) " +
            "FROM information_schema.tables " +
            "WHERE TABLE_SCHEMA = #{appId}")
    Long sumStorageBytes(@Param("appId") String appId);
}
