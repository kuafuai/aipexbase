package com.kuafuai.common.dynamic_config.service.impl;


import com.kuafuai.common.db.DynamicDataBaseServiceImpl;
import com.kuafuai.common.dynamic_config.domain.SystemConfig;
import com.kuafuai.common.dynamic_config.mapper.SysConfigMapper;
import com.kuafuai.common.dynamic_config.service.SysConfigService;
import org.springframework.stereotype.Service;

@Service
public class SysConfigServiceImpl extends DynamicDataBaseServiceImpl<SysConfigMapper, SystemConfig> implements SysConfigService {
}
