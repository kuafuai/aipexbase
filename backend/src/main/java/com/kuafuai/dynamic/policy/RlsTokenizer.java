package com.kuafuai.dynamic.policy;

import com.kuafuai.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RLS 策略词法分析器（Tokenizer）
 * 将 SQL 字符串转换为 Token 流
 */
@Slf4j
public class RlsTokenizer {

    private final String sql;
    private int position = 0;

    /**
     * 关键字映射表
     */
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("CREATE", TokenType.KEYWORD_CREATE);
        KEYWORDS.put("POLICY", TokenType.KEYWORD_POLICY);
        KEYWORDS.put("ON", TokenType.KEYWORD_ON);
        KEYWORDS.put("FOR", TokenType.KEYWORD_FOR);
        KEYWORDS.put("USING", TokenType.KEYWORD_USING);
        KEYWORDS.put("WITH", TokenType.KEYWORD_WITH);
        KEYWORDS.put("CHECK", TokenType.KEYWORD_CHECK);
        KEYWORDS.put("SELECT", TokenType.KEYWORD_SELECT);
        KEYWORDS.put("INSERT", TokenType.KEYWORD_INSERT);
        KEYWORDS.put("UPDATE", TokenType.KEYWORD_UPDATE);
        KEYWORDS.put("DELETE", TokenType.KEYWORD_DELETE);
        KEYWORDS.put("ALL", TokenType.KEYWORD_ALL);
        KEYWORDS.put("AND", TokenType.KEYWORD_AND);
        KEYWORDS.put("OR", TokenType.KEYWORD_OR);
        KEYWORDS.put("NOT", TokenType.KEYWORD_NOT);
        KEYWORDS.put("IN", TokenType.KEYWORD_IN);
        KEYWORDS.put("EXISTS", TokenType.KEYWORD_EXISTS);
        KEYWORDS.put("CASE", TokenType.KEYWORD_CASE);
        KEYWORDS.put("WHEN", TokenType.KEYWORD_WHEN);
        KEYWORDS.put("THEN", TokenType.KEYWORD_THEN);
        KEYWORDS.put("ELSE", TokenType.KEYWORD_ELSE);
        KEYWORDS.put("END", TokenType.KEYWORD_END);
        KEYWORDS.put("TRUE", TokenType.BOOLEAN_LITERAL);
        KEYWORDS.put("FALSE", TokenType.BOOLEAN_LITERAL);
    }

    public RlsTokenizer(String sql) {
        this.sql = sql;
    }

    /**
     * 执行词法分析，返回 Token 流
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (position < sql.length()) {
            skipWhitespace();
            if (position >= sql.length()) {
                break;
            }

            // 跳过注释
            if (skipComments()) {
                continue;
            }

            char c = sql.charAt(position);

            // 字符串字面量
            if (c == '\'') {
                tokens.add(readStringLiteral());
                continue;
            }

            // 双引号标识符（如 "table_name"）
            if (c == '"') {
                tokens.add(readQuotedIdentifier());
                continue;
            }

            // 标识符或关键字
            if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
                continue;
            }

            // 数字字面量
            if (Character.isDigit(c)) {
                tokens.add(readNumberLiteral());
                continue;
            }

            // 运算符和符号
            Token symbolToken = readSymbol();
            if (symbolToken != null) {
                tokens.add(symbolToken);
                continue;
            }

            // 未知字符
            throw new BusinessException("policy.tokenizer.unknown_char: '" + c + "' at position " + position);
        }

        // 添加 EOF
        tokens.add(new Token(TokenType.EOF, "", position));

        log.debug("词法分析完成，生成 {} 个 token", tokens.size());
        return tokens;
    }

    /**
     * 跳过空白字符
     */
    private void skipWhitespace() {
        while (position < sql.length() && Character.isWhitespace(sql.charAt(position))) {
            position++;
        }
    }

    /**
     * 跳过注释
     * @return 如果跳过了注释返回 true
     */
    private boolean skipComments() {
        // 行注释 --
        if (position + 1 < sql.length() &&
            sql.charAt(position) == '-' &&
            sql.charAt(position + 1) == '-') {

            position += 2;
            while (position < sql.length() && sql.charAt(position) != '\n') {
                position++;
            }
            return true;
        }

        // 块注释 /* */
        if (position + 1 < sql.length() &&
            sql.charAt(position) == '/' &&
            sql.charAt(position + 1) == '*') {

            position += 2;
            while (position + 1 < sql.length()) {
                if (sql.charAt(position) == '*' && sql.charAt(position + 1) == '/') {
                    position += 2;
                    return true;
                }
                position++;
            }
            throw new BusinessException("policy.tokenizer.unclosed_comment at position " + position);
        }

        return false;
    }

    /**
     * 读取字符串字面量（处理 SQL 的 '' 转义）
     */
    private Token readStringLiteral() {
        int startPos = position;
        position++; // 跳过开始的 '

        StringBuilder value = new StringBuilder();

        while (position < sql.length()) {
            char c = sql.charAt(position);

            if (c == '\'') {
                // 检查是否是转义的 ''
                if (position + 1 < sql.length() && sql.charAt(position + 1) == '\'') {
                    value.append('\''); // 添加一个单引号
                    position += 2; // 跳过 ''
                    continue;
                }

                // 字符串结束
                position++; // 跳过结束的 '
                return new Token(TokenType.STRING_LITERAL, value.toString(), startPos);
            }

            value.append(c);
            position++;
        }

        throw new BusinessException("policy.tokenizer.unclosed_string at position " + startPos);
    }

    /**
     * 读取双引号标识符
     */
    private Token readQuotedIdentifier() {
        int startPos = position;
        position++; // 跳过开始的 "

        StringBuilder value = new StringBuilder();

        while (position < sql.length()) {
            char c = sql.charAt(position);

            if (c == '"') {
                position++; // 跳过结束的 "
                return new Token(TokenType.IDENTIFIER, value.toString(), startPos);
            }

            value.append(c);
            position++;
        }

        throw new BusinessException("policy.tokenizer.unclosed_quoted_identifier at position " + startPos);
    }

    /**
     * 读取标识符或关键字
     */
    private Token readIdentifierOrKeyword() {
        int startPos = position;
        StringBuilder value = new StringBuilder();

        while (position < sql.length()) {
            char c = sql.charAt(position);
            if (Character.isLetterOrDigit(c) || c == '_') {
                value.append(c);
                position++;
            } else {
                break;
            }
        }

        String text = value.toString();
        String upperText = text.toUpperCase();

        // 检查是否是关键字
        TokenType type = KEYWORDS.getOrDefault(upperText, TokenType.IDENTIFIER);

        return new Token(type, text, startPos);
    }

    /**
     * 读取数字字面量
     */
    private Token readNumberLiteral() {
        int startPos = position;
        StringBuilder value = new StringBuilder();

        while (position < sql.length()) {
            char c = sql.charAt(position);
            if (Character.isDigit(c) || c == '.') {
                value.append(c);
                position++;
            } else {
                break;
            }
        }

        return new Token(TokenType.NUMBER_LITERAL, value.toString(), startPos);
    }

    /**
     * 读取运算符和符号
     */
    private Token readSymbol() {
        int startPos = position;
        char c = sql.charAt(position);

        // 单字符符号
        switch (c) {
            case '(':
                position++;
                return new Token(TokenType.LPAREN, "(", startPos);
            case ')':
                position++;
                return new Token(TokenType.RPAREN, ")", startPos);
            case ',':
                position++;
                return new Token(TokenType.COMMA, ",", startPos);
            case ';':
                position++;
                return new Token(TokenType.SEMICOLON, ";", startPos);
            case '.':
                position++;
                return new Token(TokenType.DOT, ".", startPos);
        }

        // 多字符运算符
        if (position + 1 < sql.length()) {
            String twoChar = sql.substring(position, position + 2);
            switch (twoChar) {
                case "!=":
                    position += 2;
                    return new Token(TokenType.NOT_EQUAL, "!=", startPos);
                case "<>":
                    position += 2;
                    return new Token(TokenType.NOT_EQUAL, "<>", startPos);
                case "<=":
                    position += 2;
                    return new Token(TokenType.LESS_EQUAL, "<=", startPos);
                case ">=":
                    position += 2;
                    return new Token(TokenType.GREATER_EQUAL, ">=", startPos);
            }
        }

        // 单字符运算符
        switch (c) {
            case '=':
                position++;
                return new Token(TokenType.EQUAL, "=", startPos);
            case '<':
                position++;
                return new Token(TokenType.LESS_THAN, "<", startPos);
            case '>':
                position++;
                return new Token(TokenType.GREATER_THAN, ">", startPos);
        }

        return null; // 未识别的符号
    }
}
