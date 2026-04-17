package com.kuafuai.manage.controller;

import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.AppBatchVo;
import com.kuafuai.manage.entity.vo.AppVo;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.context.ManageApiContext;
import com.kuafuai.manage.context.ManageApiContextHolder;
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
 * 支持两种场景：
 * - 场景1：用户级Token，userId固化在token中
 * - 场景2：企业级Token，userId优先从请求body获取，其次从X-User-Id header获取
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manage")
public class ManageApiController {

    private final ManageBusinessService manageBusinessService;

    /**
     * 获取当前操作人的userId
     * - 场景1（用户级token）：直接使用token绑定的userId
     * - 场景2（企业级token）：优先使用body中的userId，其次使用header中的userId
     */
    private String getUserIdFromContext(String bodyUserId) {
        ManageApiContext context = ManageApiContextHolder.get();
        if (context == null) {
            throw new BusinessException("认证信息缺失");
        }

        String userId = context.isUserToken()
                ? context.getUserId()
                : StringUtils.defaultIfEmpty(bodyUserId, context.getUserId());

        if (StringUtils.isEmpty(userId)) {
            throw new BusinessException("用户ID缺失");
        }
        return userId;
    }

    /**
     * 批量创建应用和表
     *
     * @param appBatchVo 应用信息和表列表
     * @return 创建结果（包含 appId 和 apiKey）
     */
    @PostMapping("/application/batch")
    public BaseResponse createAppWithTables(@RequestBody AppBatchVo appBatchVo) {
        if (StringUtils.isEmpty(appBatchVo.getName())) {
            throw new BusinessException("error.param.required", "name");
        }

        String userId = getUserIdFromContext(appBatchVo.getUserId());
        appBatchVo.setUserId(userId);

        log.info("对外API - 批量创建应用和表: name={}, userId={}, tables.size={}", appBatchVo.getName(), userId, appBatchVo.getTables() != null ? appBatchVo.getTables().size() : 0);

        AppInfo appInfo = manageBusinessService.createAppWithTables(appBatchVo);

        // 返回 apiKey 和 appId
        Map<String, String> result = new HashMap<>();
        result.put("apiKey", appInfo.getAppId());
        result.put("appId", appInfo.getAppId());

        return ResultUtils.success(result);
    }

    /**
     * 创建应用
     *
     * @param appVo 应用信息（需要提供 name）
     * @return 创建的应用信息
     */
    @PostMapping("/application")
    public BaseResponse createApp(@RequestBody AppVo appVo) {
        if (StringUtils.isEmpty(appVo.getName())) {
            throw new BusinessException("error.param.required", "name");
        }

        // 获取userId（场景1来自token，场景2优先body其次header）
        String userId = getUserIdFromContext(appVo.getUserId());

        log.info("对外API - 创建应用: name={}, userId={}", appVo.getName(), userId);
        AppInfo appInfo = manageBusinessService.createAppForApi(appVo.getName(), userId);

        // 返回 apiKey 和 appId
        Map<String, String> result = new HashMap<>();
        result.put("apiKey", appInfo.getAppId());
        result.put("appId", appInfo.getAppId());

        return ResultUtils.success(result);
    }

    /**
     * 删除应用
     *
     * @param appVo 应用信息（需要提供 appId）
     * @return 删除结果
     */
    @DeleteMapping("/application")
    public BaseResponse deleteApp(@RequestBody AppVo appVo) {
        if (StringUtils.isEmpty(appVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }

        log.info("对外API - 删除应用: appId={}", appVo.getAppId());
        manageBusinessService.deleteApp(appVo.getAppId());
        return ResultUtils.success();
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

    /**
     * 向应用的表中添加字段
     *
     * @param columnVo 字段信息（需要提供 appId、tableName、columnName、columnType）
     * @return 添加结果
     */
    @PostMapping("/column")
    public BaseResponse addColumn(@RequestBody ColumnVo columnVo) {
        if (StringUtils.isEmpty(columnVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
        if (StringUtils.isEmpty(columnVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        if (StringUtils.isEmpty(columnVo.getColumnName())) {
            throw new BusinessException("error.param.required", "columnName");
        }
        if (StringUtils.isEmpty(columnVo.getColumnType())) {
            throw new BusinessException("error.param.required", "columnType");
        }

        log.info("对外API - 添加字段: appId={}, tableName={}, columnName={}, columnType={}",
                columnVo.getAppId(), columnVo.getTableName(), columnVo.getColumnName(), columnVo.getColumnType());
        boolean result = manageBusinessService.addColumn(columnVo.getAppId(), columnVo.getTableName(), columnVo);
        return result ? ResultUtils.success() : ResultUtils.error("error.code.fail");
    }
}
