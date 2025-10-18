package com.kuafuai.common.dynamic_config.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuafuai.common.dynamic_config.domain.KfSystemConfig;
import com.kuafuai.common.dynamic_config.domain.SystemConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper

public interface KfSysConfigMapper extends BaseMapper<KfSystemConfig> {
}
