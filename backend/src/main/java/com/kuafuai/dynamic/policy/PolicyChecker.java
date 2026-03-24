package com.kuafuai.dynamic.policy;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * RLS 策略检查器
 * 用于 INSERT/UPDATE 操作的 WITH CHECK 验证
 */
@Slf4j
public class PolicyChecker {

    /**
     * 验证 INSERT 操作的数据是否符合 WITH CHECK 策略
     *
     * @param appId  应用ID
     * @param table  表名
     * @param data   待插入的数据
     * @throws BusinessException 如果违反策略
     */
    public static void checkInsert(String appId, String table, Map<String, Object> data) {
        checkPolicy(appId, table, "INSERT", data);
    }

    /**
     * 验证 UPDATE 操作的数据是否符合 WITH CHECK 策略
     *
     * @param appId  应用ID
     * @param table  表名
     * @param data   待更新的数据
     * @throws BusinessException 如果违反策略
     */
    public static void checkUpdate(String appId, String table, Map<String, Object> data) {
        checkPolicy(appId, table, "UPDATE", data);
    }

    /**
     * 检查数据是否符合 WITH CHECK 策略
     *
     * @param appId     应用ID
     * @param table     表名
     * @param operation 操作类型
     * @param data      数据
     * @throws BusinessException 如果违反策略
     */
    private static void checkPolicy(String appId, String table, String operation, Map<String, Object> data) {
        try {
            // 1. 获取 WITH CHECK 表达式
            List<String> expressions = PolicyEngine.getWithCheckExpressions(appId, table, operation);

            if (expressions.isEmpty()) {
                // 没有 WITH CHECK 策略，直接通过
                return;
            }

            // 2. 验证每个表达式（所有表达式都必须满足，用 AND 逻辑）
            for (String expression : expressions) {
                if (StringUtils.isEmpty(expression)) {
                    continue;
                }

                // 解析表达式（替换 auth.uid() 等内置函数）
                String parsed = PolicyExpressionParser.parse(expression);

                // 使用新的表达式求值引擎
                boolean allowed = ExpressionEvaluator.evaluate(parsed, data);

                if (!allowed) {
                    log.warn("数据违反 WITH CHECK 策略: table={}, operation={}, expression={}, data={}",
                            table, operation, expression, data);
                    throw new BusinessException("policy.with_check.violation");
                }
            }

            log.debug("数据通过 WITH CHECK 验证: table={}, operation={}", table, operation);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WITH CHECK 验证失败: appId={}, table={}, operation={}", appId, table, operation, e);
            throw new BusinessException("policy.with_check.error");
        }
    }
}
