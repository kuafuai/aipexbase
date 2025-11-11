package com.kuafuai.manage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.ImmutableMap;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.PageRequest;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.DateUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.APIKeyVo;
import com.kuafuai.manage.entity.vo.ApiMarketVo;
import com.kuafuai.manage.entity.vo.DynamicApiVo;
import com.kuafuai.manage.entity.vo.SwitchStatusVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.system.entity.APIKey;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.DynamicApiSetting;
import com.kuafuai.system.service.AppInfoService;
import com.kuafuai.system.service.ApplicationAPIKeysService;
import com.kuafuai.system.service.DynamicApiSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/application/config")
public class ApplicationConfigsController {

    private static final Map<Integer, String> METHOD_MAP = ImmutableMap.<Integer, String>builder()
            .put(0, "GET")
            .put(1, "POST")
            .put(2, "PUT")
            .put(3, "DELETE")
            .put(4, "PATCH")
            .build();

    private static final Map<Integer, String> DATA_TYPE_MAP = ImmutableMap.<Integer, String>builder()
            .put(0, "TEXT")
            .put(1, "JSON")
            .put(2, "ARRAY")
            .build();


    private static final Map<Integer, String> PROTOCOL_MAP = ImmutableMap.<Integer, String>builder()
            .put(1, "HTTP")
            .put(2, "WSS")
            .build();

    private final AppInfoService appInfoService;

    @Resource
    private DynamicApiSettingService dynamicApiSettingService;

    private final ManageBusinessService manageBusinessService;

    @Resource
    private ApplicationAPIKeysService applicationAPIKeysService;

    /**
     * 获取 第三方服务配置
     */

    @PostMapping("/dynamicapi/{id}/page")
    public BaseResponse listThirdPartApi(@RequestBody PageRequest pageRequest, @PathVariable("id") String appId) {

        IPage<DynamicApiSetting> page = new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize());

        QueryWrapper<DynamicApiSetting> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", appId);

        return ResultUtils.success(dynamicApiSettingService.page(page, queryWrapper));
    }

    /**
     * 创建 第三方服务配置
     */

    @PostMapping("/dynamicapi/{id}/save")
    public BaseResponse saveThirdPartApi(@RequestBody DynamicApiVo dynamicApiVo, @PathVariable("id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        if (StringUtils.isAnyEmpty(dynamicApiVo.getKeyName(), dynamicApiVo.getUrl(), dynamicApiVo.getDataRaw(), dynamicApiVo.getMethod())) {
            return ResultUtils.error("error.code.params_error");
        }

        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getKeyName, dynamicApiVo.getKeyName());
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);

        if (Objects.nonNull(dynamicApiSettingService.getOne(queryWrapper))) {
            return ResultUtils.error("error.data.exist");
        }

        DynamicApiSetting json = DynamicApiSetting.builder()
                .appId(appInfo.getAppId())
                .id(null)
                .bodyTemplate(dynamicApiVo.getBodyTemplate())
                .bodyType("template")
                .keyName(dynamicApiVo.getKeyName())
                .description(dynamicApiVo.getDescription())
                .url(dynamicApiVo.getUrl())
                .method(dynamicApiVo.getMethod())
                .protocol("http")
                .dataRaw(dynamicApiVo.getDataRaw())
                .header(dynamicApiVo.getHeaderTemplate())
                .varRaw(dynamicApiVo.getVars())
                .build();

        return ResultUtils.success(dynamicApiSettingService.save(json));
    }

    @DeleteMapping("/dynamicapi/{id}/delete/{key}")
    public BaseResponse deleteThirdPartApi(@PathVariable("key") String keyName, @PathVariable("id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getKeyName, keyName);
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);

        return ResultUtils.success(dynamicApiSettingService.remove(queryWrapper));
    }


    @PostMapping("/dynamicapi/{id}/saveApiMarket")
    public BaseResponse saveApi(@PathVariable("id") String appId, @RequestBody ApiMarketVo marketVo) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        LambdaQueryWrapper<DynamicApiSetting> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DynamicApiSetting::getKeyName, marketVo.getName());
        queryWrapper.eq(DynamicApiSetting::getAppId, appId);

        if (Objects.nonNull(dynamicApiSettingService.getOne(queryWrapper))) {
            return ResultUtils.error("error.data.exist");
        }

        DynamicApiSetting json = DynamicApiSetting.builder()
                .appId(appInfo.getAppId())
                .marketId(marketVo.getId())
                .keyName(marketVo.getName())
                .description(marketVo.getDescription())
                .url(marketVo.getUrl())
                .protocol(PROTOCOL_MAP.get(marketVo.getProtocol()))
                .method(METHOD_MAP.get(marketVo.getMethod()))
                .dataType(DATA_TYPE_MAP.get(marketVo.getDataType()))
                .dataPath(marketVo.getDataPath())
                .build();

        return ResultUtils.success(dynamicApiSettingService.save(json));
    }

    /**
     * 创建 api key
     */

    @PostMapping("/apikeys/{id}/save")
    public BaseResponse saveOneApiKey(@PathVariable("id") String appId, @RequestBody APIKeyVo apiKeyVo) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        if (StringUtils.isEmpty(apiKeyVo.getName())) {
            return ResultUtils.error("error.code.params_error");
        }

        if (!StringUtils.isEmpty(apiKeyVo.getExpireAt())) {
            if (apiKeyVo.getExpireAt().compareTo(DateUtils.getTime()) <= 0) {
                return ResultUtils.error("error.code.params_error");
            }
        }

        return ResultUtils.success(manageBusinessService.saveAPIKey(appId, apiKeyVo));
    }


    @PostMapping("/apikeys/{id}/page")
    public BaseResponse listApiKeys(@RequestBody PageRequest pageRequest, @PathVariable("id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        IPage<APIKey> page = new Page<>(pageRequest.getCurrent(), pageRequest.getPageSize());

        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(APIKey::getAppId, appId);
        page = applicationAPIKeysService.page(page, queryWrapper);
        return ResultUtils.success(page);
    }


    @PostMapping("/apikeys/{id}/switch")
    public BaseResponse switchStatus(@RequestBody SwitchStatusVo switchStatusVo, @PathVariable("id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        if (!StringUtils.containsAny(Arrays.asList(APIKey.APIKeyStatus.ACTIVE.name(), APIKey.APIKeyStatus.DISABLE.name()), switchStatusVo.getStatus())) {
            throw new BusinessException("error.code.params_error");
        }

        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(APIKey::getAppId, appId);
        queryWrapper.eq(APIKey::getKeyName, switchStatusVo.getKeyName());


        APIKey apiKey = applicationAPIKeysService.getOne(queryWrapper);
        if (Objects.isNull(apiKey)) {
            throw new BusinessException("error.code.params_error");
        }

        if (StringUtils.equals(switchStatusVo.getStatus(), apiKey.getStatus())) {
            return ResultUtils.success();
        }

        apiKey.setStatus(switchStatusVo.getStatus());
        return ResultUtils.success(applicationAPIKeysService.updateById(apiKey));
    }

    @DeleteMapping("/apikeys/{id}/{keyName}")
    public BaseResponse delete(@PathVariable("keyName") String keyName, @PathVariable("id") String appId) {

        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (Objects.isNull(appInfo)) {
            throw new BusinessException("error.code.not_found");
        }

        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }

        LambdaQueryWrapper<APIKey> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(APIKey::getAppId, appId);
        queryWrapper.eq(APIKey::getKeyName, keyName);

        return ResultUtils.success(applicationAPIKeysService.remove(queryWrapper));
    }
}
