package com.kuafuai.login.controller;


import com.google.common.collect.ImmutableMap;
import com.kuafuai.common.domin.BaseResponse;
import com.kuafuai.common.domin.ErrorCode;
import com.kuafuai.common.domin.ResultUtils;
import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.text.Convert;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.login.domain.Login;
import com.kuafuai.login.handle.DynamicAuthFilter;
import com.kuafuai.login.handle.GlobalAppIdFilter;
import com.kuafuai.login.service.LoginService;
import com.kuafuai.system.DynamicInfoCache;
import com.kuafuai.system.entity.AppInfo;
import com.kuafuai.system.entity.AppTableColumnInfo;
import com.kuafuai.system.entity.AppTableInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/login_manger")
public class LoginMangerController {

    @Autowired
    private DynamicInfoCache dynamicInfoCache;

    @Autowired
    private LoginService loginService;

    @GetMapping("relevance/all")
    @ApiOperation("关联业务类型")
    public BaseResponse relevance_all() {
        String appId = GlobalAppIdFilter.getAppId();
        AppInfo appInfo = DynamicAuthFilter.getAppInfo();

        if (appInfo.getNeedAuth()) {
            String authTable = appInfo.getAuthTable();
            AppTableInfo tableInfo = dynamicInfoCache.getAppTableInfo(appId, authTable);
            if (tableInfo != null) {
                Map<String, Object> map = ImmutableMap.of(
                        "label", tableInfo.getDescription(),
                        "value", StringUtils.dbStrToHumpLower(tableInfo.getTableName())
                );
                return ResultUtils.success(Collections.singletonList(map));
            }
        }
        return ResultUtils.success(Collections.emptyList());
    }

    public final List<String> usernameKeys = Arrays.asList("username", "userName", "user_name", "usrname", "uname", "accountName", "acctName");

    @GetMapping("relevance/get_value")
    @ApiOperation("关联业务类型")
    public BaseResponse get_relevance_value(@RequestParam(value = "relevance") String relevance) {
        String tableName = StringUtils.humpToDbStr(relevance);
        String appId = GlobalAppIdFilter.getAppId();
        List<AppTableColumnInfo> columnInfos = dynamicInfoCache.getAppTableColumnInfo(appId, tableName);
        if (!columnInfos.isEmpty()) {
            Optional<Object> optional = findKeys(columnInfos, usernameKeys)
                    .map(Optional::of)
                    .orElseGet(() -> findDsl(columnInfos))
                    .map(Optional::of)
                    .orElseGet(() -> columnInfos.stream()
                            .filter(AppTableColumnInfo::isPrimary)
                            .findFirst()
                            .map(AppTableColumnInfo::getColumnName));

            return ResultUtils.success(ImmutableMap.of(
                    "api", tableName + ".list",
                    "show_name", optional.get()
            ));
        } else {
            return ResultUtils.error("not find");
        }
    }

    @PostMapping("update_password")
    @ApiOperation("修改密码")
    public BaseResponse update_password(@RequestBody Map<String, Object> data) {
        String appId = GlobalAppIdFilter.getAppId();
        final Integer loginId = Convert.toInt(data.get("login_id"));
        final String password = Convert.toStr(data.getOrDefault("password", "123456"));
        if (loginId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (org.apache.commons.lang3.StringUtils.isEmpty(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        final String encryptPassword = SecurityUtils.encryptPassword(password);

        final Login entity = new Login();
        entity.setLoginId(loginId);
        entity.setPassword(encryptPassword);
        boolean flag = this.loginService.updateById(appId, entity);
        return flag ? ResultUtils.success() : ResultUtils.error(ErrorCode.OPERATION_ERROR);
    }

    private Optional<Object> findDsl(List<AppTableColumnInfo> columnInfos) {
        return columnInfos.stream()
                .filter(p -> StringUtils.equalsAnyIgnoreCase(p.getDslType(), "email", "phone"))
                .findFirst()
                .map(AppTableColumnInfo::getColumnName);
    }

    private Optional<Object> findKeys(List<AppTableColumnInfo> columnInfos, List<String> keys) {
        return columnInfos.stream()
                .filter(p -> keys.contains(p.getColumnName()))
                .findFirst()
                .map(AppTableColumnInfo::getColumnName);
    }


}
