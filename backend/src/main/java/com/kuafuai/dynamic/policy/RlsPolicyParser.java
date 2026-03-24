package com.kuafuai.dynamic.policy;

import com.kuafuai.common.exception.BusinessException;
import com.kuafuai.common.util.StringUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RLS 策略解析器（入口）
 * 使用基于 Token 流的两阶段解析：
 * 1. 词法分析（Tokenizer）：字符串 → Token 流
 * 2. 语法分析（Parser）：Token 流 → ParsedPolicy
 *
 * 支持的语法格式：
 * CREATE POLICY "policy_name" ON table_name FOR SELECT USING (expression);
 * CREATE POLICY "policy_name" ON table_name FOR INSERT WITH CHECK (expression);
 * CREATE POLICY "policy_name" ON table_name FOR ALL USING (expr1) WITH CHECK (expr2);
 *
 * 优势：
 * - 清晰的分层架构（词法/语法分离）
 * - 支持复杂表达式（子查询、嵌套函数等）
 * - 易于扩展和维护
 */
@Slf4j
public class RlsPolicyParser {

    /**
     * 解析结果
     */
    @Data
    @Builder
    public static class ParsedPolicy {
        private String policyName;
        private String tableName;
        private String operation;      // SELECT, INSERT, UPDATE, DELETE, ALL
        private String usingExpression;
        private String withCheckExpression;
    }

    /**
     * 解析 CREATE POLICY 语句
     *
     * @param sql CREATE POLICY SQL 语句
     * @return 解析结果
     * @throws BusinessException 如果语法错误
     */
    public static ParsedPolicy parse(String sql) {
        if (StringUtils.isEmpty(sql)) {
            throw new BusinessException("policy.syntax.empty");
        }

        log.debug("开始解析 RLS 策略: {}", sql);

        try {
            // 阶段 1: 词法分析
            RlsTokenizer tokenizer = new RlsTokenizer(sql);
            List<Token> tokens = tokenizer.tokenize();

            log.debug("词法分析生成 {} 个 token", tokens.size());

            // 阶段 2: 语法分析
            RlsParser parser = new RlsParser(tokens);
            ParsedPolicy result = parser.parse();

            log.info("RLS 策略解析成功: {}", result);
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("RLS 策略解析失败: {}", sql, e);
            throw new BusinessException("policy.parse.error: " + e.getMessage());
        }
    }
}
