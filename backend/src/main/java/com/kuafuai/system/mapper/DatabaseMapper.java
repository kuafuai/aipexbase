package com.kuafuai.system.mapper;


import com.kuafuai.system.entity.vo.DatabaseStructureVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DatabaseMapper {

    int createDatabase(@Param("appId") String appId);
    int deleteDatabase(@Param("appId") String appId);

    int createDDL(@Param("sql") String sql);

    List<DatabaseStructureVO.ColumnStructureVO> getTableColumns(@Param("appId") String appId, @Param("tableName") String tableName);
}
