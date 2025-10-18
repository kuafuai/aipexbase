package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.DynamicApiSetting;

public interface DynamicApiSettingService extends IService<DynamicApiSetting> {

    boolean deleteByAppId(String appId);
}
