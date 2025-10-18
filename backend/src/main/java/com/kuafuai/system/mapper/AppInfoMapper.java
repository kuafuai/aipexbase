package com.kuafuai.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.system.entity.AppInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppInfoMapper extends BaseMapper<AppInfo> {
    @Select("SELECT DATABASE();")
    String show();
}
