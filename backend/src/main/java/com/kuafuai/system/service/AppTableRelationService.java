package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.AppTableRelation;

public interface AppTableRelationService extends IService<AppTableRelation> {

    boolean deleteByAppId(String appId);

    boolean deleteByAppIdAndTableId(String appId, Long tableId);

    boolean deleteByAppId(String appId, String requirementId);
}
