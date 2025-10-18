package com.kuafuai.api.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.DynamicApiSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api_setting_manger")
public class ApiManagerController {

    @Autowired
    private DynamicApiSettingService dynamicApiSettingService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody Map<String, Object> data) {
        String appId = GlobalAppIdFilter.getAppId();
        IPage<DynamicApiSetting> page = new Page<>(
                Convert.toInt(data.getOrDefault("current", "1")),
                Convert.toInt(data.getOrDefault("pageSize", "10")));

        QueryWrapper<DynamicApiSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId);
        queryWrapper.eq("`show`", 1);


        return ResultUtils.success(dynamicApiSettingService.page(page, queryWrapper));
    }

    @PutMapping("update")
    public BaseResponse update(@RequestBody DynamicApiSetting apiSetting) {
        String appId = GlobalAppIdFilter.getAppId();
        apiSetting.setAppId(appId);

        dynamicApiSettingService.updateById(apiSetting);
        return ResultUtils.success();
    }

}
