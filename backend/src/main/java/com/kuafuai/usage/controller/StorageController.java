package com.kuafuai.usage.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.usage.service.StorageQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * 项目文件资产: 上传过的所有文件 (从 file_upload_log 出).
 * 只读, 只 owner 可查.
 */
@RestController
@RequestMapping("/admin/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageQueryService storageQueryService;
    private final AppInfoService appInfoService;

    @GetMapping("/{appId}/summary")
    public BaseResponse<?> summary(@PathVariable String appId) {
        checkAppPermission(appId);
        return ResultUtils.success(storageQueryService.summary(appId));
    }

    @GetMapping("/{appId}/breakdown")
    public BaseResponse<?> breakdown(@PathVariable String appId) {
        checkAppPermission(appId);
        return ResultUtils.success(storageQueryService.breakdown(appId));
    }

    @GetMapping("/{appId}/files")
    public BaseResponse<?> files(@PathVariable String appId,
                                 @RequestParam(required = false) String kind,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam(defaultValue = "1") long current,
                                 @RequestParam(defaultValue = "20") long pageSize) {
        checkAppPermission(appId);
        return ResultUtils.success(storageQueryService.pageFiles(appId, kind, keyword, current, pageSize));
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
