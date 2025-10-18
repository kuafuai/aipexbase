package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppTableRelation;
import com.kuafuai.system.mapper.AppTableRelationMapper;
import com.kuafuai.system.service.AppTableRelationService;
import org.springframework.stereotype.Service;

@Service
public class AppTableRelationServiceImpl extends ServiceImpl<AppTableRelationMapper, AppTableRelation> implements AppTableRelationService {
    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<AppTableRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableRelation::getAppId, appId);
        return remove(queryWrapper);
    }

    @Override
    public boolean deleteByAppIdAndTableId(String appId, Long tableId) {
        LambdaQueryWrapper<AppTableRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableRelation::getAppId, appId);
        queryWrapper.eq(AppTableRelation::getTableId, tableId);

        return remove(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(String appId, String requirementId) {
        LambdaQueryWrapper<AppTableRelation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppTableRelation::getAppId, appId);
        queryWrapper.eq(AppTableRelation::getRequirementId, requirementId);

        return remove(queryWrapper);
    }
}
