package com.kuafuai.dynamic.policy;

import com.kuafuai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * RLS 策略语法分析器（Parser）
 * 使用递归下降解析，将 Token 流转换为结构化的 ParsedPolicy 对象
 *
 * 语法规则：
 * policy := CREATE POLICY policy_name ON table_name [FOR operation] [USING expression] [WITH CHECK expression]
 */
@Slf4j
public class RlsParser {

    private final List<Token> tokens;
    private int currentPos = 0;

    public RlsParser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * 解析策略
     */
    public RlsPolicyParser.ParsedPolicy parse() {
        // CREATE POLICY
        expect(TokenType.KEYWORD_CREATE, "Expected CREATE");
        expect(TokenType.KEYWORD_POLICY, "Expected POLICY");

        // policy_name (可以是标识符或字符串字面量)
        String policyName = parsePolicyName();

        // ON table_name
        expect(TokenType.KEYWORD_ON, "Expected ON");
        String tableName = parseTableName();

        // [FOR operation]
        String operation = parseOperation();

        // [USING expression]
        String usingExpression = parseUsingClause();

        // [WITH CHECK expression]
        String withCheckExpression = parseWithCheckClause();

        // 验证结果
        validatePolicy(policyName, tableName, operation, usingExpression, withCheckExpression);

        RlsPolicyParser.ParsedPolicy result = RlsPolicyParser.ParsedPolicy.builder()
                .policyName(policyName)
                .tableName(tableName)
                .operation(operation != null ? operation : "ALL")
                .usingExpression(usingExpression)
                .withCheckExpression(withCheckExpression)
                .build();

        log.info("语法分析完成: {}", result);
        return result;
    }

    /**
     * 解析策略名（可以是标识符或字符串字面量）
     */
    private String parsePolicyName() {
        Token token = current();
        if (token.getType() == TokenType.IDENTIFIER || token.getType() == TokenType.STRING_LITERAL) {
            advance();
            return token.getValue();
        }
        throw new BusinessException("policy.parser.expected_policy_name at position " + token.getPosition());
    }

    /**
     * 解析表名
     */
    private String parseTableName() {
        Token token = current();
        if (token.getType() == TokenType.IDENTIFIER) {
            advance();
            return token.getValue();
        }
        throw new BusinessException("policy.parser.expected_table_name at position " + token.getPosition());
    }

    /**
     * 解析操作类型（可选）
     */
    private String parseOperation() {
        if (current().getType() != TokenType.KEYWORD_FOR) {
            return null; // 没有 FOR 子句，默认 ALL
        }

        advance(); // 跳过 FOR

        Token opToken = current();
        switch (opToken.getType()) {
            case KEYWORD_SELECT:
            case KEYWORD_INSERT:
            case KEYWORD_UPDATE:
            case KEYWORD_DELETE:
            case KEYWORD_ALL:
                advance();
                return opToken.getValue().toUpperCase();
            default:
                throw new BusinessException("policy.parser.invalid_operation at position " + opToken.getPosition());
        }
    }

    /**
     * 解析 USING 子句（可选）
     */
    private String parseUsingClause() {
        if (current().getType() != TokenType.KEYWORD_USING) {
            return null;
        }

        advance(); // 跳过 USING

        // 期望一个括号包裹的表达式
        expect(TokenType.LPAREN, "Expected '(' after USING");

        // 提取括号内的表达式（收集所有 token 直到匹配的右括号）
        String expression = extractParenthesizedExpression();

        return expression;
    }

    /**
     * 解析 WITH CHECK 子句（可选）
     */
    private String parseWithCheckClause() {
        if (current().getType() != TokenType.KEYWORD_WITH) {
            return null;
        }

        advance(); // 跳过 WITH
        expect(TokenType.KEYWORD_CHECK, "Expected CHECK after WITH");

        // 期望一个括号包裹的表达式
        expect(TokenType.LPAREN, "Expected '(' after WITH CHECK");

        // 提取括号内的表达式
        String expression = extractParenthesizedExpression();

        return expression;
    }

    /**
     * 提取括号内的表达式
     * 当前位置应该在 LPAREN 之后
     * 返回表达式字符串（重建从 token）
     */
    private String extractParenthesizedExpression() {
        int depth = 1; // 已经消费了开始的 (
        StringBuilder expr = new StringBuilder();

        while (currentPos < tokens.size() && depth > 0) {
            Token token = current();

            if (token.getType() == TokenType.EOF) {
                throw new BusinessException("policy.parser.unmatched_parenthesis");
            }

            // 1. 先 append 当前 token
            if (token.getType() == TokenType.LPAREN) {
                depth++;
                expr.append("(");
            } else if (token.getType() == TokenType.RPAREN) {
                depth--;
                if (depth > 0) {
                    expr.append(")");
                }
                // 注意：depth == 0 时不 append，这是匹配的外层右括号
            } else {
                expr.append(tokenToString(token));
            }

            // 2. 前进到下一个 token
            advance();

            // 3. 统一检查：当前 token 和下一个 token 之间是否需要空格
            if (depth > 0 && currentPos < tokens.size()) {
                Token next = current();
                if (needSpace(token, next)) {
                    expr.append(" ");
                }
            }
        }

        return expr.toString().trim();
    }

    /**
     * 判断两个 token 之间是否需要空格
     * 默认需要空格，只列举不需要空格的情况
     */
    private boolean needSpace(Token prev, Token next) {
        if (prev == null || next == null) {
            return false;
        }

        TokenType prevType = prev.getType();
        TokenType nextType = next.getType();

        // 不需要空格的情况：

        // 1. 点号前后：auth.uid
        if (prevType == TokenType.DOT || nextType == TokenType.DOT) {
            return false;
        }

        // 2. 左括号后、右括号前：(auth), (x)
        if (prevType == TokenType.LPAREN || nextType == TokenType.RPAREN) {
            return false;
        }

        // 3. 函数调用：标识符 + 左括号不需要空格：uid(
        //    但关键字 + 左括号需要空格：AND (, CASE (
        if (prevType == TokenType.IDENTIFIER && nextType == TokenType.LPAREN) {
            return false;
        }

        // 4. 逗号前：a,b
        if (nextType == TokenType.COMMA) {
            return false;
        }

        // 默认需要空格（关键字、运算符、标识符之间）
        return true;
    }

    /**
     * 将 Token 转换回字符串表示
     */
    private String tokenToString(Token token) {
        switch (token.getType()) {
            case STRING_LITERAL:
                // 字符串需要加引号，并转义内部的单引号
                return "'" + token.getValue().replace("'", "''") + "'";
            case IDENTIFIER:
                // 标识符保持原样（如果需要引号，原始 SQL 中应该已有）
                return token.getValue();
            case NUMBER_LITERAL:
            case BOOLEAN_LITERAL:
                return token.getValue();
            case EQUAL:
                return "=";
            case NOT_EQUAL:
                return token.getValue(); // != 或 <>
            case LESS_THAN:
                return "<";
            case LESS_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_EQUAL:
                return ">=";
            case COMMA:
                return ",";
            case DOT:
                return ".";
            default:
                // 关键字和其他
                return token.getValue();
        }
    }

    /**
     * 验证解析结果
     */
    private void validatePolicy(String policyName, String tableName, String operation,
                                 String usingExpr, String withCheckExpr) {
        if (policyName == null || policyName.isEmpty()) {
            throw new BusinessException("policy.parser.missing_policy_name");
        }

        if (tableName == null || tableName.isEmpty()) {
            throw new BusinessException("policy.parser.missing_table_name");
        }

        // 至少需要 USING 或 WITH CHECK 之一
        if ((usingExpr == null || usingExpr.isEmpty()) &&
            (withCheckExpr == null || withCheckExpr.isEmpty())) {
            throw new BusinessException("policy.parser.missing_expression");
        }

        // SELECT/DELETE 不支持 WITH CHECK
        if (("SELECT".equalsIgnoreCase(operation) || "DELETE".equalsIgnoreCase(operation)) &&
            withCheckExpr != null && !withCheckExpr.isEmpty()) {
            throw new BusinessException("policy.parser.invalid_with_check_for_select_delete");
        }
    }

    // ========== Token 流操作辅助方法 ==========

    /**
     * 获取当前 token
     */
    private Token current() {
        if (currentPos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // 返回 EOF
        }
        return tokens.get(currentPos);
    }

    /**
     * 前进到下一个 token
     */
    private void advance() {
        if (currentPos < tokens.size() - 1) {
            currentPos++;
        }
    }

    /**
     * 期望特定类型的 token
     */
    private void expect(TokenType expectedType, String errorMessage) {
        Token token = current();
        if (token.getType() != expectedType) {
            throw new BusinessException(errorMessage + ", but got " + token.getType() +
                    " at position " + token.getPosition());
        }
        advance();
    }
}
