package com.kuafuai.manage.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.AppVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.system.entity.AppInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 对外开放的管理API接口
 * 需要通过 X-Manage-Token 请求头或 token 参数进行身份验证
 * 需要在配置文件中设置 manage.api.enable=true 和 manage.api.token
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manage")
public class ManageApiController {

    private final ManageBusinessService manageBusinessService;

    /**
     * 创建应用
     *
     * @param appVo 应用信息（需要提供 name 和 owner）
     * @return 创建的应用信息
     */
    @PostMapping("/application")
    public BaseResponse createApp(@RequestBody AppVo appVo) {
        if (StringUtils.isEmpty(appVo.getName())) {
            throw new BusinessException("error.param.required", "name");
        }
        if (StringUtils.isEmpty(appVo.getUserId())) {
            throw new BusinessException("error.param.required", "owner");
        }

        log.info("对外API - 创建应用: name={}, owner={}", appVo.getName(), appVo.getUserId());
        AppInfo appInfo = manageBusinessService.createAppForApi(appVo.getName(), appVo.getUserId());

        // 返回 apiKey 和 appId
        Map<String, String> result = new HashMap<>();
        result.put("apiKey", appInfo.getAppId());
        result.put("appId", appInfo.getAppId());

        return ResultUtils.success(result);
    }

    /**
     * 创建表
     *
     * @param tableVo 表信息（需要提供 appId、tableName 等信息）
     * @return 创建结果
     */
    @PostMapping("/table")
    public BaseResponse createTable(@RequestBody TableVo tableVo) {
        if (StringUtils.isEmpty(tableVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
        if (StringUtils.isEmpty(tableVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }

        log.info("对外API - 创建表: appId={}, tableName={}", tableVo.getAppId(), tableVo.getTableName());
        boolean result = manageBusinessService.createTable(tableVo.getAppId(), tableVo);
        return result ? ResultUtils.success() : ResultUtils.error("error.code.fail");
    }
}
