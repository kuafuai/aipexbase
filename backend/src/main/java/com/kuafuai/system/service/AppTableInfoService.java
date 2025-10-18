package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.AppTableInfo;

import java.util.List;

public interface AppTableInfoService extends IService<AppTableInfo> {

    boolean deleteByAppId(String appId);

    boolean deleteByAppIdAndTableName(String appId,String tableName);


    List<AppTableInfo> getByAppIdAndRequirementId(String appId,String requirementId);

    List<AppTableInfo> getByAppId(String appId);

    boolean deleteByAppId(String appId,String requirementId);

    boolean existTableNameByAppId(String appId, String configTableName);
}
