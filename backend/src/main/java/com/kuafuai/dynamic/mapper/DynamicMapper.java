package com.kuafuai.dynamic.mapper;

import com.kuafuai.dynamic.service.DynamicSQLBuilder;
import com.kuafuai.dynamic.service.DynamicSqlProvider;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DynamicMapper {

    /**
     * 分页查询
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @SelectProvider(type = DynamicSQLBuilder.class, method = "buildDynamicSelect")
    List<Map<String, Object>> page(@Param("database") String database,
                                   @Param("table") String table,
                                   @Param("conditions") Map<String, Object> conditions);

    /**
     * 查询数量
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @SelectProvider(type = DynamicSQLBuilder.class, method = "buildDynamicCount")
    int count(@Param("database") String database,
              @Param("table") String table,
              @Param("conditions") Map<String, Object> conditions);


    /**
     * 获取list
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @SelectProvider(type = DynamicSQLBuilder.class, method = "buildDynamicList")
    List<Map<String, Object>> list(@Param("database") String database,
                                   @Param("table") String table,
                                   @Param("conditions") Map<String, Object> conditions);


    /**
     * 查询一条记录
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @SelectProvider(type = DynamicSQLBuilder.class, method = "buildDynamicOne")
    Map<String, Object> getOne(@Param("database") String database,
                               @Param("table") String table,
                               @Param("conditions") Map<String, Object> conditions);

    /**
     * 插入
     *
     * @param database
     * @param table
     * @param conditions
     */
    @InsertProvider(type = DynamicSQLBuilder.class, method = "buildDynamicInsert")
    void insert(@Param("database") String database,
                @Param("table") String table,
                @Param("conditions") Map<String, Object> conditions);

    @InsertProvider(type = DynamicSqlProvider.class, method = "buildDynamicInsertBatch")
    int insertBatch(@Param("database") String database,
                @Param("table") String table,
                @Param("conditions") List<Map<String, Object>> conditions);

    @DeleteProvider(type = DynamicSqlProvider.class, method = "buildDynamicDeleteBatch")
    int deleteBatch(@Param("database") String database,
                    @Param("table") String table,
                    @Param("conditions") Map<String, Object> conditions);

    /**
     * 获取插入id
     *
     * @return
     */
    @Select("SELECT LAST_INSERT_ID()")
    long getLastId();

    /**
     * 删除
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @DeleteProvider(type = DynamicSQLBuilder.class, method = "buildDynamicDelete")
    int delete(@Param("database") String database,
               @Param("table") String table,
               @Param("conditions") Map<String, Object> conditions);

    /**
     * 跟新
     *
     * @param database
     * @param table
     * @param conditions
     * @return
     */
    @UpdateProvider(type = DynamicSQLBuilder.class, method = "buildDynamicUpdate")
    int update(@Param("database") String database,
               @Param("table") String table,
               @Param("conditions") Map<String, Object> conditions);
}
