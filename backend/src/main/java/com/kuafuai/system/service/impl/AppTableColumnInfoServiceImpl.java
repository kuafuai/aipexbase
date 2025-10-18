package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.mapper.AppTableColumnInfoMapper;
import com.kuafuai.system.service.AppTableColumnInfoService;
import org.springframework.stereotype.Service;

@Service
public class AppTableColumnInfoServiceImpl extends ServiceImpl<AppTableColumnInfoMapper, AppTableColumnInfo> implements AppTableColumnInfoService {

    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<AppTableColumnInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableColumnInfo::getAppId, appId);

        return remove(queryWrapper);
    }

    @Override
    public boolean deleteByAppIdAndTableId(String appId, Long tableId) {
        LambdaQueryWrapper<AppTableColumnInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableColumnInfo::getAppId, appId);
        queryWrapper.eq(AppTableColumnInfo::getTableId, tableId);

        return remove(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(String appId, String requirementId) {
        LambdaQueryWrapper<AppTableColumnInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableColumnInfo::getAppId, appId);
        queryWrapper.eq(AppTableColumnInfo::getRequirementId, requirementId);

        return remove(queryWrapper);
    }
}
