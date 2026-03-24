package com.kuafuai.dynamic.policy;

import com.kuafuai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * RLS 表达式求值引擎
 * 基于 Token 流，支持复杂表达式的求值
 *
 * 支持的运算：
 * - 逻辑运算：AND, OR, NOT
 * - 比较运算：=, !=, <, >, <=, >=
 * - IN 操作符
 * - 函数调用（通过 PolicyExpressionParser 预处理）
 *
 * 示例：
 * - "'123' = user_id" + {user_id: '123'} → true
 * - "age >= 18 AND status = 'active'" + {age: 20, status: 'active'} → true
 * - "(role = 'admin' OR role = 'moderator') AND enabled = true" → 根据数据求值
 */
@Slf4j
public class ExpressionEvaluator {

    private final List<Token> tokens;
    private final Map<String, Object> data;
    private int currentPos = 0;

    public ExpressionEvaluator(List<Token> tokens, Map<String, Object> data) {
        this.tokens = tokens;
        this.data = data;
    }

    /**
     * 对表达式求值
     *
     * @param expression 表达式字符串（auth.uid() 已被替换）
     * @param data       数据上下文
     * @return 表达式的布尔值结果
     */
    public static boolean evaluate(String expression, Map<String, Object> data) {
        try {
            // 1. 词法分析
            RlsTokenizer tokenizer = new RlsTokenizer(expression);
            List<Token> tokens = tokenizer.tokenize();

            // 2. 求值
            ExpressionEvaluator evaluator = new ExpressionEvaluator(tokens, data);
            return evaluator.evaluateExpression();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("表达式求值失败: expression={}, data={}", expression, data, e);
            throw new BusinessException("policy.expression.eval_error: " + e.getMessage());
        }
    }

    /**
     * 递归下降求值：表达式 = OR 表达式
     */
    private boolean evaluateExpression() {
        return evaluateOr();
    }

    /**
     * OR 表达式：AND (OR AND)*
     */
    private boolean evaluateOr() {
        boolean result = evaluateAnd();

        while (current().getType() == TokenType.KEYWORD_OR) {
            advance(); // 跳过 OR
            boolean right = evaluateAnd();
            result = result || right;
        }

        return result;
    }

    /**
     * AND 表达式：NOT (AND NOT)*
     */
    private boolean evaluateAnd() {
        boolean result = evaluateNot();

        while (current().getType() == TokenType.KEYWORD_AND) {
            advance(); // 跳过 AND
            boolean right = evaluateNot();
            result = result && right;
        }

        return result;
    }

    /**
     * NOT 表达式：[NOT] 比较表达式
     */
    private boolean evaluateNot() {
        if (current().getType() == TokenType.KEYWORD_NOT) {
            advance(); // 跳过 NOT
            return !evaluateComparison();
        }
        return evaluateComparison();
    }

    /**
     * 比较表达式：值 比较运算符 值 | 值 IN (值列表) | (表达式)
     */
    private boolean evaluateComparison() {
        // 处理括号
        if (current().getType() == TokenType.LPAREN) {
            advance(); // 跳过 (
            boolean result = evaluateExpression();
            expect(TokenType.RPAREN, "Expected ')'");
            return result;
        }

        // 左值
        Object leftValue = evaluateValue();

        Token op = current();

        // 处理 IN 操作符
        if (op.getType() == TokenType.KEYWORD_IN) {
            advance(); // 跳过 IN
            return evaluateIn(leftValue);
        }

        // 处理比较运算符
        if (isComparisonOperator(op.getType())) {
            advance(); // 跳过运算符
            Object rightValue = evaluateValue();
            return evaluateComparisonOp(leftValue, op.getType(), rightValue);
        }

        throw new BusinessException("policy.expression.unexpected_token: " + op);
    }

    /**
     * 求值：值（字面量或字段引用）
     */
    private Object evaluateValue() {
        Token token = current();

        switch (token.getType()) {
            case STRING_LITERAL:
                advance();
                return token.getValue();

            case NUMBER_LITERAL:
                advance();
                return parseNumber(token.getValue());

            case BOOLEAN_LITERAL:
                advance();
                return Boolean.parseBoolean(token.getValue());

            case IDENTIFIER:
                advance();
                // 从数据上下文中获取字段值
                return data.get(token.getValue());

            default:
                throw new BusinessException("policy.expression.unexpected_value_token: " + token);
        }
    }

    /**
     * 求值：IN 操作符
     */
    private boolean evaluateIn(Object value) {
        expect(TokenType.LPAREN, "Expected '(' after IN");

        // 收集 IN 列表中的值
        List<Object> inList = new java.util.ArrayList<>();

        while (current().getType() != TokenType.RPAREN && current().getType() != TokenType.EOF) {
            inList.add(evaluateValue());

            if (current().getType() == TokenType.COMMA) {
                advance(); // 跳过逗号
            }
        }

        expect(TokenType.RPAREN, "Expected ')' after IN list");

        // 检查值是否在列表中
        return inList.stream().anyMatch(item -> compareEqual(value, item));
    }

    /**
     * 执行比较运算
     */
    private boolean evaluateComparisonOp(Object left, TokenType op, Object right) {
        switch (op) {
            case EQUAL:
                return compareEqual(left, right);

            case NOT_EQUAL:
                return !compareEqual(left, right);

            case LESS_THAN:
                return compareNumbers(left, right) < 0;

            case LESS_EQUAL:
                return compareNumbers(left, right) <= 0;

            case GREATER_THAN:
                return compareNumbers(left, right) > 0;

            case GREATER_EQUAL:
                return compareNumbers(left, right) >= 0;

            default:
                throw new BusinessException("policy.expression.unknown_operator: " + op);
        }
    }

    /**
     * 比较相等（支持字符串和数字）
     */
    private boolean compareEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null || right == null) return false;

        // 统一转换为字符串比较（避免类型不匹配问题）
        return String.valueOf(left).equals(String.valueOf(right));
    }

    /**
     * 数字比较
     */
    private int compareNumbers(Object left, Object right) {
        if (left == null || right == null) {
            throw new BusinessException("policy.expression.null_in_comparison");
        }

        try {
            double leftNum = parseNumber(String.valueOf(left));
            double rightNum = parseNumber(String.valueOf(right));
            return Double.compare(leftNum, rightNum);
        } catch (NumberFormatException e) {
            throw new BusinessException("policy.expression.invalid_number_comparison: " + left + " vs " + right);
        }
    }

    /**
     * 解析数字
     */
    private double parseNumber(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("policy.expression.invalid_number: " + value);
        }
    }

    /**
     * 判断是否是比较运算符
     */
    private boolean isComparisonOperator(TokenType type) {
        return type == TokenType.EQUAL ||
               type == TokenType.NOT_EQUAL ||
               type == TokenType.LESS_THAN ||
               type == TokenType.LESS_EQUAL ||
               type == TokenType.GREATER_THAN ||
               type == TokenType.GREATER_EQUAL;
    }

    // ========== Token 流操作辅助方法 ==========

    private Token current() {
        if (currentPos >= tokens.size()) {
            return tokens.get(tokens.size() - 1); // 返回 EOF
        }
        return tokens.get(currentPos);
    }

    private void advance() {
        if (currentPos < tokens.size() - 1) {
            currentPos++;
        }
    }

    private void expect(TokenType expectedType, String errorMessage) {
        Token token = current();
        if (token.getType() != expectedType) {
            throw new BusinessException(errorMessage + ", but got " + token.getType() +
                    " at position " + token.getPosition());
        }
        advance();
    }
}
