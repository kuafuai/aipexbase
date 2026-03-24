package com.kuafuai.dynamic.policy;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * auth 命名空间实现
 * <p>
 * 支持的方法：
 * - auth.uid() : 当前登录用户的 relevanceId
 */
@Slf4j
public class AuthNamespace implements FunctionNamespace {

    @Override
    public String getName() {
        return "auth";
    }

    @Override
    public String evaluate(String methodName) {
        LoginUser loginUser = SecurityUtils.getLoginUser();

        if (loginUser == null) {
            log.warn("auth.{} 调用失败: 未登录", methodName);
            return "NULL";
        }

        switch (methodName) {
            case "uid":
                return evaluateUid(loginUser);

            case "user_id":
                return evaluateUserId(loginUser);

            case "tenant_id":
                return evaluateTenantId(loginUser);

            case "app_id":
                return evaluateAppId(loginUser);

            case "table":
                return evaluateTable(loginUser);

            default:
                log.warn("未知的 auth 方法: {}", methodName);
                return "NULL";
        }
    }

    /**
     * auth.uid() - 返回 relevanceId
     */
    private String evaluateUid(LoginUser loginUser) {
        String userId = loginUser.getRelevanceId();
        if (StringUtils.isEmpty(userId)) {
            return "NULL";
        }
        return "'" + escapeSql(userId) + "'";
    }

    /**
     * auth.user_id() - 返回 userId
     */
    private String evaluateUserId(LoginUser loginUser) {
        Long userId = loginUser.getUserId();
        if (userId == null) {
            return "NULL";
        }
        return String.valueOf(userId);
    }

    /**
     * auth.tenant_id() - 返回 tenantId
     */
    private String evaluateTenantId(LoginUser loginUser) {
        Integer tenantId = loginUser.getTenantId();
        if (tenantId == null) {
            return "NULL";
        }
        return String.valueOf(tenantId);
    }

    /**
     * auth.app_id() - 返回 appId
     */
    private String evaluateAppId(LoginUser loginUser) {
        String appId = loginUser.getAppId();
        if (StringUtils.isEmpty(appId)) {
            return "NULL";
        }
        return "'" + escapeSql(appId) + "'";
    }

    /**
     * auth.table() - 返回 relevanceTable
     */
    private String evaluateTable(LoginUser loginUser) {
        String table = loginUser.getRelevanceTable();
        if (StringUtils.isEmpty(table)) {
            return "NULL";
        }
        return "'" + escapeSql(table) + "'";
    }

    /**
     * SQL 转义
     */
    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
