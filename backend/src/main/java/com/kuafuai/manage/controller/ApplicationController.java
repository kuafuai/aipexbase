package com.kuafuai.manage.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.dto.AppInfoDTO;
import com.kuafuai.manage.entity.vo.AppVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/application")
public class ApplicationController {


    private final AppInfoService appInfoService;
    private final ManageBusinessService manageBusinessService;

    /**
     * 应用列表
     */
    @PostMapping("/page")
    public BaseResponse page(@RequestBody AppVo appVo) {
        LambdaQueryWrapper<AppInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppInfo::getOwner, SecurityUtils.getUserId());
        if (StringUtils.isNotEmpty(appVo.getName())) {
            queryWrapper.like(AppInfo::getAppName, appVo.getName());
        }
        queryWrapper.orderByDesc(AppInfo::getId);
        Page<AppInfo> page = new Page<>(appVo.getCurrent(), appVo.getPageSize());
        return ResultUtils.success(appInfoService.page(page, queryWrapper));
    }

    /**
     * 创建应用
     */
    @PostMapping
    public BaseResponse create(@RequestBody AppVo appVo) {

        if (StringUtils.isEmpty(appVo.getName())) {
            throw new BusinessException("error.param.required", "name");
        }
        AppInfo appInfo = manageBusinessService.createApp(appVo);
        return ResultUtils.success(appInfo);
    }

    @PatchMapping("/recycle/{id}")
    public BaseResponse recycle(@PathVariable(value = "id") String appId) {
        //todo 临时方案，后续走定时任务
        manageBusinessService.recycle(appId);
        return ResultUtils.success();
    }

    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    public BaseResponse delete(@PathVariable(value = "id") String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }
        manageBusinessService.deleteApp(appId);
        return ResultUtils.success();
    }

    /**
     * 应用概览
     */
    @GetMapping("/overview")
    public BaseResponse overview(@RequestParam(value = "id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("error.code.not_found");
        }

        // 获取应用下表数据
        List<AppTableInfo> tablesByAppId = manageBusinessService.getTablesByAppId(TableVo.builder().appId(appId).build());


        return ResultUtils.success(AppInfoDTO.builder()
                .appName(appInfo.getAppName())
                .appId(appInfo.getAppId())
                .owner(appInfo.getOwner())
                .description(appInfo.getDescription())
                .iconUrl(appInfo.getIconUrl())
                .status(appInfo.getStatus())
                .needAuth(appInfo.getNeedAuth())
                .authTable(appInfo.getAuthTable())
                .configJson(appInfo.getConfigJson())
                .appTableNum(tablesByAppId.size())
                .build());
    }

    /**
     * 更新应用基础信息
     */
    @PatchMapping("/update/{id}")
    public BaseResponse update(@RequestBody AppVo appVo, @PathVariable(value = "id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        if (StringUtils.isEmpty(appVo.getAppName())) {
            throw new BusinessException("error.param.required", "appName");
        }
        appInfo.setAppName(appVo.getAppName());
        appInfo.setNeedAuth(appVo.getNeedAuth());
        appInfo.setAuthTable(appVo.getAuthTable());
        appInfo.setConfigJson(appVo.getConfigJson());
        return ResultUtils.success(appInfoService.updateAppInfoByAppId(appInfo));
    }
}
