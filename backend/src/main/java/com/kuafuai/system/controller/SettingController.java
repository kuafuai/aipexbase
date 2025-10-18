package com.kuafuai.system.controller;

import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.service.AppTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/system")
public class SettingController {

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;


    @Autowired
    private AppTableInfoService appTableInfoService;

    private final static   String newTableName = "kf_system_config";


    /**
     * 查询 key 的 系统配置
     *
     * @param key
     * @return
     */
    @GetMapping("/setting/{key}")
    public BaseResponse settingByKey(@PathVariable("key") String key) {
        String appId = GlobalAppIdFilter.getAppId();
        String table = "system_config";


        boolean existTableNameByAppId = appTableInfoService.existTableNameByAppId(appId, newTableName);
        if (existTableNameByAppId){
            table=newTableName;
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("name", key);

        return ResultUtils.success(dynamicInterfaceService.list(appId, table, params));
    }
}
