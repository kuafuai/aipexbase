package com.kuafuai.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuafuai.system.entity.AppInfo;

public interface AppInfoService extends IService<AppInfo> {

    String show();

    AppInfo getAppInfoByAppId(String appId);

    boolean deleteByAppId(String appId);

    boolean updateAppInfoByAppId(AppInfo appInfo);
}
