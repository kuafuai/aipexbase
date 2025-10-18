package com.kuafuai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.mapper.DynamicApiSettingMapper;
import com.kuafuai.system.service.DynamicApiSettingService;
import org.springframework.stereotype.Service;

@Service
public class DynamicApiSettingServiceImpl extends ServiceImpl<DynamicApiSettingMapper, DynamicApiSetting>
        implements DynamicApiSettingService {
    @Override
    public boolean deleteByAppId(String appId) {
        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);

        return remove(queryWrapper);
    }
}
