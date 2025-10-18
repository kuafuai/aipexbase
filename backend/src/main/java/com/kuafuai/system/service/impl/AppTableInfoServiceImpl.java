package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppTableInfo;
import com.kuafuai.system.mapper.AppTableInfoMapper;
import com.kuafuai.system.service.AppTableInfoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppTableInfoServiceImpl extends ServiceImpl<AppTableInfoMapper, AppTableInfo> implements AppTableInfoService {
    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId);

        return remove(queryWrapper);
    }

    @Override
    public boolean deleteByAppIdAndTableName(String appId, String tableName) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId);
        queryWrapper.eq(AppTableInfo::getTableName, tableName);

        return remove(queryWrapper);
    }

    @Override
    public List<AppTableInfo> getByAppIdAndRequirementId(String appId, String requirementId) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId);
        queryWrapper.eq(AppTableInfo::getRequirementId, requirementId);

        return list(queryWrapper);
    }

    @Override
    public List<AppTableInfo> getByAppId(String appId) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId);

        return list(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(String appId, String requirementId) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId);
        queryWrapper.eq(AppTableInfo::getRequirementId, requirementId);

        return remove(queryWrapper);
    }

    @Override
    public boolean existTableNameByAppId(String appId, String tableName) {
        LambdaQueryWrapper<AppTableInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableInfo::getAppId, appId).eq(AppTableInfo::getTableName, tableName);
        
        long count = count(queryWrapper);
        return count > 0;
    }
}
