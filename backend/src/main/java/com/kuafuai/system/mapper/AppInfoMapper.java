package com.kuafuai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.system.entity.AppInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {
    @Select("SELECT DATABASE();")
    String show();

    @Update("UPDATE app_info " +
            "SET updated_at = NOW() " +
            "WHERE app_id = #{appId} " +
            "AND (updated_at < CURDATE())")
    int updateLastActive(String appId);
}
