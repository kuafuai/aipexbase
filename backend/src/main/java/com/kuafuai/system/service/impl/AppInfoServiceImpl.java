package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.mapper.AppInfoMapper;
import com.kuafuai.system.service.AppInfoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AppInfoServiceImpl extends ServiceImpl<AppInfoMapper, AppInfo> implements AppInfoService {
    @Resource
    private AppInfoMapper appInfoMapper;

    @Override
    public String show() {
        return appInfoMapper.show();
    }

    @Override
    public AppInfo getAppInfoByAppId(String appId) {
        LambdaQueryWrapper<AppInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppInfo::getAppId, appId);

        return getOne(queryWrapper);
    }

    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<AppInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppInfo::getAppId, appId);

        return remove(queryWrapper);
    }

    @Override
    public boolean updateAppInfoByAppId(AppInfo appInfo) {
        return updateById(appInfo);
    }
}
