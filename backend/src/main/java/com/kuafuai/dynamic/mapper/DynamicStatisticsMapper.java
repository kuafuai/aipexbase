package com.kuafuai.dynamic.mapper;


import com.kuafuai.dynamic.service.DynamicSQLBuilder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;
import java.util.Map;

@Mapper
public interface DynamicStatisticsMapper {



    @SelectProvider(type = DynamicSQLBuilder.class, method = "buildDynamicStatisticsCount")
    List<Map<String,Object>> count(@Param("database") String database,
                                   @Param("table") String table,
                                   @Param("conditions") Map<String, Object> conditions);
}
