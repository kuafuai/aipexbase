package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.AppRequirementSQL;

public interface AppRequirementSQLService extends IService<AppRequirementSQL> {

    boolean deleteByAppId(String appId);
}
