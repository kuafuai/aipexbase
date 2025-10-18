package com.kuafuai.manage.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.dynamic.service.DynamicInterfaceService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.AppTableInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/admin/system"})
@RequiredArgsConstructor
@Slf4j
public class ManageSettingController {

    private final DynamicInterfaceService dynamicInterfaceService;
    private final AppTableInfoService appTableInfoService;
    private final AppInfoService appInfoService;

    private final static String newTableName = "kf_system_config";

    @GetMapping("/settings/{appId}")
    public BaseResponse settings(@PathVariable("appId") String appId) {

        checkAppPermission(appId);

        String table = "system_config";
        boolean existTableNameByAppId = appTableInfoService.existTableNameByAppId(appId, newTableName);
        if (existTableNameByAppId) {
            table = newTableName;
        }
        return ResultUtils.success(dynamicInterfaceService.list(appId, table, Maps.newHashMap()));
    }

    @PostMapping("/settings/{appId}")
    public BaseResponse saveSetting(@PathVariable("appId") String appId, @RequestBody Map<String, Object> data) {
        checkAppPermission(appId);
        //1. 先根据 data 的 key 删除数据
        //2. 在根据 data 的 key 保存数据
        String table = "system_config";
        boolean existTableNameByAppId = appTableInfoService.existTableNameByAppId(appId, newTableName);
        if (existTableNameByAppId) {
            table = newTableName;
        }

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

    private void checkAppPermission(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("请检查应用是否存在");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("无权操作此应用");
        }
    }
}
