package com.kuafuai.system.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.service.AppTableInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system")
public class SettingController {

    @Autowired
    private DynamicInterfaceService dynamicInterfaceService;


    @Autowired
    private AppTableInfoService appTableInfoService;

    private final static String newTableName = "kf_system_config";
    private final static String MASK_VALUE = "******";


    /**
     * 查询 key 的 系统配置
     *
     * @param key
     * @return
     */
    @GetMapping("/setting/{key}")
    public BaseResponse settingByKey(@PathVariable("key") String key) {
        String appId = GlobalAppIdFilter.getAppId();
        String table = checkDefaultSettingTable(appId);

        Map<String, Object> params = Maps.newHashMap();
        params.put("name", key);

        return ResultUtils.success(dynamicInterfaceService.list(appId, table, params));
    }

    @GetMapping("/settings")
    public BaseResponse settings() {

        String appId = GlobalAppIdFilter.getAppId();
        String table = checkDefaultSettingTable(appId);
        return ResultUtils.success(dynamicInterfaceService.list(appId, table, Maps.newHashMap()));
    }

    @PostMapping("/settings")
    public BaseResponse saveSetting(@RequestBody Map<String, Object> data) {

        // 过滤掉值为 "******" 的字段，不更新这些被掩码的敏感数据
        data.entrySet().removeIf(entry -> MASK_VALUE.equals(entry.getValue()));

        if (data.isEmpty()) {
            return ResultUtils.success();
        }
        String appId = GlobalAppIdFilter.getAppId();
        String table = checkDefaultSettingTable(appId);

        List<String> keys = Lists.newArrayList(data.keySet());
        Map<String, Object> deleteCond = Maps.newHashMap();
        deleteCond.put("name", keys);
        dynamicInterfaceService.delete(appId, table, deleteCond);

        List<Map<String, Object>> insertCond = keys.stream().map(key -> {
            Map<String, Object> map = Maps.newHashMap();
            map.put("name", key);
            map.put("content", data.get(key));
            return map;
        }).collect(Collectors.toList());

        dynamicInterfaceService.addBatch(appId, table, insertCond);
        return ResultUtils.success();
    }

    private String checkDefaultSettingTable(String appId) {
        String table = "system_config";
        boolean existTableNameByAppId = appTableInfoService.existTableNameByAppId(appId, newTableName);
        if (existTableNameByAppId) {
            table = newTableName;
        }
        return table;
    }
}
