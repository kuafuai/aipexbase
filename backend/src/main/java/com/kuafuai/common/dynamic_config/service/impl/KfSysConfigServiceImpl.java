package com.kuafuai.common.dynamic_config.service.impl;


import com.kuafuai.common.db.DynamicDataBaseServiceImpl;
import com.kuafuai.common.dynamic_config.domain.KfSystemConfig;
import com.kuafuai.common.dynamic_config.mapper.KfSysConfigMapper;
import com.kuafuai.common.dynamic_config.service.KfSysConfigService;
import org.springframework.stereotype.Service;

@Service
public class KfSysConfigServiceImpl extends DynamicDataBaseServiceImpl<KfSysConfigMapper, KfSystemConfig> implements KfSysConfigService {
}
