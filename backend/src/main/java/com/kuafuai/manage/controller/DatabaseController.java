package com.kuafuai.manage.controller;


import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.manage.entity.vo.ColumnVo;
import com.kuafuai.manage.entity.vo.DataVo;
import com.kuafuai.manage.entity.vo.TableVo;
import com.kuafuai.manage.service.ManageBusinessService;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.service.AppInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/admin/database")
@RequiredArgsConstructor
@Slf4j
public class DatabaseController {

    private final ManageBusinessService manageBusinessService;
    private final AppInfoService appInfoService;

    /**
     * 应用下的所有表
     */
    @PostMapping("/tables")
    public BaseResponse tables(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        checkAppPermission(tableVo.getAppId());

        return ResultUtils.success(manageBusinessService.getTablesByAppId(tableVo));
    }

    /**
     * 表下的字段
     */
    @PostMapping("/columns")
    public BaseResponse columns(@RequestBody ColumnVo columnVo) {
        validateColumnVo(columnVo);
        checkAppPermission(columnVo.getAppId());

        return ResultUtils.success(manageBusinessService.getColumnsByAppIdAndTableId(columnVo));
    }

    /**
     * 表的数据--分页
     */
    @PostMapping("/data")
    public BaseResponse data(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        if (StringUtils.isEmpty(tableVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        checkAppPermission(tableVo.getAppId());

        return ResultUtils.success(manageBusinessService.getTableData(tableVo));
    }

    /**
     * 表的数据--下拉列表
     */
    @PostMapping("/select")
    public BaseResponse select(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        if (StringUtils.isEmpty(tableVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        checkAppPermission(tableVo.getAppId());
        return ResultUtils.success(manageBusinessService.getTableSelect(tableVo));
    }

    /**
     * 获取表的详细信息
     */
    @PostMapping("/get/table")
    public BaseResponse getTable(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        checkAppPermission(tableVo.getAppId());
        return ResultUtils.success(manageBusinessService.getTable(tableVo));
    }

    @PostMapping("/update/table")
    public BaseResponse updateTable(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        checkAppPermission(tableVo.getAppId());
        return manageBusinessService.updateTable(tableVo.getAppId(), tableVo) ? ResultUtils.success() : ResultUtils.error("error.code.fail");
    }

    /**
     * 创建新表
     */
    @PostMapping("/create/table")
    public BaseResponse createTable(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        checkAppPermission(tableVo.getAppId());
        return manageBusinessService.createTable(tableVo.getAppId(), tableVo) ? ResultUtils.success() : ResultUtils.error("error.code.fail");
    }

    @PostMapping("/delete/table")
    public BaseResponse deleteTable(@RequestBody TableVo tableVo) {
        validateTableVo(tableVo);
        checkAppPermission(tableVo.getAppId());
        return manageBusinessService.deleteTable(tableVo.getAppId(), tableVo) ? ResultUtils.success() : ResultUtils.error("error.code.fail");
    }

    @PostMapping("/data/add")
    public BaseResponse addData(@RequestBody DataVo dataVo) {
        if (dataVo == null || StringUtils.isEmpty(dataVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
        if (StringUtils.isEmpty(dataVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        checkAppPermission(dataVo.getAppId());

        return manageBusinessService.saveData(dataVo.getAppId(), dataVo.getTableName(), dataVo.getData());
    }

    @PostMapping("/data/update")
    public BaseResponse updateData(@RequestBody DataVo dataVo) {
        if (dataVo == null || StringUtils.isEmpty(dataVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
        if (StringUtils.isEmpty(dataVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        checkAppPermission(dataVo.getAppId());
        return manageBusinessService.updateData(dataVo.getAppId(), dataVo.getTableName(), dataVo.getData());
    }

    @PostMapping("/data/delete")
    public BaseResponse deleteData(@RequestBody DataVo dataVo) {
        if (dataVo == null || StringUtils.isEmpty(dataVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
        if (StringUtils.isEmpty(dataVo.getTableName())) {
            throw new BusinessException("error.param.required", "tableName");
        }
        checkAppPermission(dataVo.getAppId());
        return manageBusinessService.deleteData(dataVo.getAppId(), dataVo.getTableName(), dataVo.getData());
    }


    private void checkAppPermission(String appId) {
        AppInfo appInfo = appInfoService.getAppInfoByAppId(appId);
        if (appInfo == null) {
            throw new BusinessException("error.code.not_login");
        }
        if (!Objects.equals(appInfo.getOwner(), SecurityUtils.getUserId())) {
            throw new BusinessException("error.code.no_auth");
        }
    }

    /**
     * 校验TableVo参数
     */
    private void validateTableVo(TableVo tableVo) {
        if (tableVo == null || StringUtils.isEmpty(tableVo.getAppId())) {
            throw new BusinessException("error.param.required", "appId");
        }
    }

    /**
     * 校验ColumnVo参数
     */
    private void validateColumnVo(ColumnVo columnVo) {
        if (columnVo == null || StringUtils.isEmpty(columnVo.getAppId()) || Objects.isNull(columnVo.getTableId())) {
            throw new BusinessException("error.param.required", "appId");
        }
    }
}
