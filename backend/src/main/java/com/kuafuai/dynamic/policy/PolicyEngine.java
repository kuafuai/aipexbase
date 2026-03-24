package com.kuafuai.dynamic.policy;

import com.kuafuai.common.login.LoginUser;
import com.kuafuai.common.login.SecurityUtils;
import com.kuafuai.common.util.SpringUtils;
import com.kuafuai.common.util.StringUtils;
import com.kuafuai.system.entity.RlsPolicy;
import com.kuafuai.system.service.RlsPolicyService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RLS 策略执行引擎
 * 负责查询、解析和合并多个策略
 */
@Slf4j
public class PolicyEngine {

    /**
     * 获取 USING 表达式（用于 SELECT/UPDATE/DELETE 的 WHERE 条件）
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @param operation 操作类型：SELECT, UPDATE, DELETE, ALL
     * @return 解析后的 SQL 条件，多个策略用 OR 连接；无策略返回空字符串
     */
    public static String getUsingCondition(String appId, String tableName, String operation) {
        try {
            // 0. 检查是否绕过 RLS
            if (shouldBypassRls()) {
                log.debug("当前用户设置了 bypassRls，跳过 RLS 检查");
                return "";
            }

            // 1. 查询该表该操作的所有启用策略
            List<RlsPolicy> policies = queryPolicies(appId, tableName, operation);

            if (policies.isEmpty()) {
                log.debug("表 {} 的 {} 操作没有配置策略，保持现状", tableName, operation);
                return "";
            }

            // 2. 解析每个策略的 USING 表达式
            List<String> parsedConditions = new ArrayList<>();
            for (RlsPolicy policy : policies) {
                String usingExpr = policy.getUsingExpression();
                if (StringUtils.isNotEmpty(usingExpr)) {
                    String parsed = PolicyExpressionParser.parse(usingExpr);
                    if (StringUtils.isNotEmpty(parsed)) {
                        parsedConditions.add("(" + parsed + ")");
                        log.debug("策略 [{}] 解析成功: {}", policy.getPolicyName(), parsed);
                    }
                }
            }

            if (parsedConditions.isEmpty()) {
                log.warn("表 {} 的 {} 操作有策略但均解析失败，拒绝访问", tableName, operation);
                return "1=0";
            }

            // 3. 多个策略用 OR 连接（满足任一策略即可）
            String result = String.join(" OR ", parsedConditions);
            log.info("表 {} 的 {} 操作应用了 {} 个策略: {}", tableName, operation, parsedConditions.size(), result);
            return result;

        } catch (Exception e) {
            log.error("获取策略失败: appId={}, table={}, operation={}", appId, tableName, operation, e);
            // 发生异常时拒绝访问
            return "1=0";
        }
    }

    /**
     * 获取 WITH CHECK 表达式（用于 INSERT/UPDATE 的数据验证）
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @param operation 操作类型：INSERT, UPDATE, ALL
     * @return WITH CHECK 表达式列表
     */
    public static List<String> getWithCheckExpressions(String appId, String tableName, String operation) {
        try {
            // 0. 检查是否绕过 RLS
            if (shouldBypassRls()) {
                log.debug("当前用户设置了 bypassRls，跳过 WITH CHECK 检查");
                return new ArrayList<>();
            }

            // 1. 查询该表该操作的所有启用策略
            List<RlsPolicy> policies = queryPolicies(appId, tableName, operation);

            if (policies.isEmpty()) {
                log.debug("表 {} 的 {} 操作没有配置 WITH CHECK 策略", tableName, operation);
                return new ArrayList<>();
            }

            // 2. 收集 WITH CHECK 表达式
            List<String> expressions = policies.stream()
                    .map(RlsPolicy::getWithCheckExpression)
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.toList());

            if (!expressions.isEmpty()) {
                log.info("表 {} 的 {} 操作应用了 {} 个 WITH CHECK 策略", tableName, operation, expressions.size());
            }

            return expressions;

        } catch (Exception e) {
            log.error("获取 WITH CHECK 策略失败: appId={}, table={}, operation={}", appId, tableName, operation, e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询策略
     *
     * @param appId     应用ID
     * @param tableName 表名
     * @param operation 操作类型
     * @return 策略列表（按优先级降序）
     */
    private static List<RlsPolicy> queryPolicies(String appId, String tableName, String operation) {
        RlsPolicyService service = SpringUtils.getBean(RlsPolicyService.class);
        return service.getPoliciesByTableAndOperation(appId, tableName, operation);
    }

    /**
     * 判断当前用户是否应该绕过 RLS 检查
     *
     * @return true: 绕过 RLS；false: 正常应用 RLS
     */
    private static boolean shouldBypassRls() {
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null) {
                return false;
            }
            Boolean bypassRls = loginUser.getBypassRls();
            return bypassRls != null && bypassRls;
        } catch (Exception e) {
            log.warn("检查 bypassRls 标志失败，默认应用 RLS", e);
            return false;
        }
    }
}
