package com.kuafuai.common.dynamic_config.service;

import com.google.common.collect.Maps;
import com.kuafuai.common.cache.Cache;
import com.kuafuai.common.dynamic_config.domain.SystemConfig;
import com.kuafuai.system.service.AppTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DynamicConfigBusinessService {


    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private KfSysConfigService kfSysConfigService;

    @Autowired
    private AppTableInfoService appTableInfoService;


    @Resource
    private Cache cache;

    private static final String CACHE_KEY_CONFIG = "config:";
    private static final  String configTableName="kf_system_config";

    public Map<String, String> getSystemConfig(String appId) {
        Map<String, String> map = Maps.newHashMap();

        List<? extends SystemConfig> list = cache.getCacheList(CACHE_KEY_CONFIG + appId);
        if (list == null) {

            boolean isExist = appTableInfoService.existTableNameByAppId(appId,configTableName);
            if (isExist){
//              如果存在，说明是新的，然后可以直接查询kf_system_config，否则查询system_config
                list = kfSysConfigService.list(appId);
            }else {
                list = sysConfigService.list(appId);
            }

            cache.setCacheList(CACHE_KEY_CONFIG + appId, list, 60, TimeUnit.SECONDS);
        }

        if (list != null && !list.isEmpty()) {
            list.forEach(p -> map.putIfAbsent(p.getName(), p.getContent()));
        }
        return map;
    }

}
