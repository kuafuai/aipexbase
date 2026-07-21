package com.kuafuai.usage.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.usage.service.UsageQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 项目用量查询: dashboard 展示用. 数据来源纯粹是 usage_hourly (主库) + information_schema (租户库).
 * 计费金额不在这里, 走 api_billing_record 的自己的端点.
 *
 * 权限: 必须是 appId 的 owner 才能查.
 */
@RestController
@RequestMapping("/admin/usage")
@RequiredArgsConstructor
@Slf4j
public class UsageController {

    private final UsageQueryService usageQueryService;
    private final AppInfoService appInfoService;

    @GetMapping("/{appId}/summary")
    public BaseResponse<?> summary(@PathVariable String appId) {
        checkAppPermission(appId);
        return ResultUtils.success(usageQueryService.summary(appId));
    }

    @GetMapping("/{appId}/timeseries")
    public BaseResponse<?> timeseries(@PathVariable String appId,
                                     @RequestParam(defaultValue = "7d") String range) {
        checkAppPermission(appId);
        return ResultUtils.success(usageQueryService.timeseries(appId, range));
    }

    @GetMapping("/{appId}/top-endpoints")
    public BaseResponse<?> topEndpoints(@PathVariable String appId,
                                       @RequestParam(defaultValue = "7d") String range,
                                       @RequestParam(defaultValue = "5") int limit) {
        checkAppPermission(appId);
        if (limit <= 0 || limit > 50) {
            limit = 5;
        }
        return ResultUtils.success(usageQueryService.topEndpoints(appId, range, limit));
    }

    private void checkAppPermission(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("error.code.not_found");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }
    }
}
