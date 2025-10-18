package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppRequirementSQL;
import com.kuafuai.system.mapper.AppRequirementSQLMapper;
import com.kuafuai.system.service.AppRequirementSQLService;
import org.springframework.stereotype.Service;

@Service
public class AppRequirementSQLServiceImpl extends ServiceImpl<AppRequirementSQLMapper, AppRequirementSQL>
        implements AppRequirementSQLService {

    @Override
    public boolean deleteByAppId(String appId) {

        LambdaQueryWrapper<AppRequirementSQL> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppRequirementSQL::getAppId, appId);

        return remove(queryWrapper);
    }
}
