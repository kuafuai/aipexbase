package com.kuafuai.dynamic.policy;

import com.kuafuai.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 策略表达式解析器（重构版）
 * 基于 Token 流，正确识别和替换内置函数
 * <p>
 * 支持的命名空间：
 * - auth: 认证相关函数
 * - auth.uid() : 当前登录用户 relevanceId
 * - auth.user_id() : 当前登录用户 userId
 * - auth.tenant_id() : 当前租户ID
 * - auth.app_id() : 当前应用ID
 * - auth.table() : 当前用户关联表名
 * <p>
 * 示例：
 * 输入: "auth.uid() = user_id"
 * 输出: "'123' = user_id"
 * <p>
 * 优势：
 * - 不会误替换字符串内的函数名
 * - 易于扩展新函数（在命名空间类中添加 case）
 * - 易于扩展新命名空间（实现 FunctionNamespace 接口并注册）
 * - 与整体 Token 流架构一致
 */
@Slf4j
public class PolicyExpressionParser {

    /**
     * 解析策略表达式，替换内置函数为实际值
     *
     * @param expression 策略表达式，如 "auth.uid() = user_id"
     * @return 解析后的 SQL 条件，如 "'123' = user_id"；如果解析失败返回 "1=0"（拒绝访问）
     */
    public static String parse(String expression) {
        if (StringUtils.isEmpty(expression)) {
            return "";
        }

        // 安全检查
        if (!isSafeExpression(expression)) {
            log.warn("策略表达式包含危险内容，已拒绝: {}", expression);
            return "1=0";
        }

        try {
            // 1. 词法分析
            RlsTokenizer tokenizer = new RlsTokenizer(expression);
            List<Token> tokens = tokenizer.tokenize();

            // 2. 遍历 Token 流，识别并替换函数调用
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);

                // 跳过 EOF
                if (token.getType() == TokenType.EOF) {
                    break;
                }

                // 检查是否是函数调用模式：identifier.identifier()
                if (token.getType() == TokenType.IDENTIFIER &&
                        i + 4 < tokens.size() &&
                        tokens.get(i + 1).getType() == TokenType.DOT &&
                        tokens.get(i + 2).getType() == TokenType.IDENTIFIER &&
                        tokens.get(i + 3).getType() == TokenType.LPAREN &&
                        tokens.get(i + 4).getType() == TokenType.RPAREN) {

                    // 解析命名空间和方法名
                    String namespace = token.getValue();
                    String method = tokens.get(i + 2).getValue();

                    // 检查是否是已注册的命名空间
                    if (FunctionRegistry.hasNamespace(namespace)) {
                        // 求值函数（返回 RlsValue）
                        RlsValue rlsValue = FunctionRegistry.evaluateFunction(namespace, method);

                        // 转换为 SQL 字面量
                        String sqlLiteral = rlsValue.toSqlLiteral();
                        result.append(sqlLiteral);

                        log.debug("替换函数: {}.{}() → {} (类型: {})", namespace, method, sqlLiteral, rlsValue.getType());

                        i += 4;
                        continue;
                    }
                }

                // 非函数调用，原样输出
                result.append(tokenToString(token));

                // 根据下一个 token 决定是否加空格
                if (i + 1 < tokens.size()) {
                    Token next = tokens.get(i + 1);
                    if (next.getType() != TokenType.EOF && needSpace(token, next)) {
                        result.append(" ");
                    }
                }
            }

            String output = result.toString().trim();
            log.debug("策略表达式解析成功: {} → {}", expression, output);
            return output;

        } catch (Exception e) {
            log.error("策略表达式解析失败: {}", expression, e);
            // 解析失败时返回 false 条件，拒绝访问
            return "1=0";
        }
    }

    /**
     * 将 Token 转换为字符串
     */
    private static String tokenToString(Token token) {
        switch (token.getType()) {
            case STRING_LITERAL:
                return "'" + token.getValue().replace("'", "''") + "'";
            case LPAREN:
                return "(";
            case RPAREN:
                return ")";
            case COMMA:
                return ",";
            case DOT:
                return ".";
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
            default:
                return token.getValue();
        }
    }

    /**
     * 判断两个 token 之间是否需要空格
     */
    private static boolean needSpace(Token current, Token next) {
        TokenType currentType = current.getType();
        TokenType nextType = next.getType();

        // 点号前后不需要空格
        if (currentType == TokenType.DOT || nextType == TokenType.DOT) {
            return false;
        }

        // 左括号后、右括号前不需要空格
        if (currentType == TokenType.LPAREN || nextType == TokenType.RPAREN) {
            return false;
        }

        // 标识符 + 左括号不需要空格（函数调用）
        if (currentType == TokenType.IDENTIFIER && nextType == TokenType.LPAREN) {
            return false;
        }

        // 逗号前不需要空格
        if (nextType == TokenType.COMMA) {
            return false;
        }

        // 默认需要空格
        return true;
    }

    /**
     * 验证表达式的安全性
     * 防止注入危险的 SQL 语句
     *
     * @param expression 表达式
     * @return 是否安全
     */
    public static boolean isSafeExpression(String expression) {
        if (StringUtils.isEmpty(expression)) {
            return true;
        }

        // 不允许包含危险关键字
        String upperExpr = expression.toUpperCase();
        String[] dangerousKeywords = {
                "DROP ", "TRUNCATE ", "ALTER ",
                "CREATE ", "GRANT ", "REVOKE ",
                "EXEC", "EXECUTE",
                "SCRIPT", "JAVASCRIPT",
                ";--", "DELETE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperExpr.contains(keyword)) {
                log.warn("策略表达式包含危险关键字: {} in {}", keyword, expression);
                return false;
            }
        }

        return true;
    }
}
