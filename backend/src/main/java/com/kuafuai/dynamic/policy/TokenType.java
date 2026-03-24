package com.kuafuai.dynamic.policy;

/**
 * Token 类型枚举
 */
public enum TokenType {
    // 关键字
    KEYWORD_CREATE,
    KEYWORD_POLICY,
    KEYWORD_ON,
    KEYWORD_FOR,
    KEYWORD_USING,
    KEYWORD_WITH,
    KEYWORD_CHECK,
    KEYWORD_SELECT,
    KEYWORD_INSERT,
    KEYWORD_UPDATE,
    KEYWORD_DELETE,
    KEYWORD_ALL,

    // SQL 运算符和关键字（用于表达式）
    KEYWORD_AND,
    KEYWORD_OR,
    KEYWORD_NOT,
    KEYWORD_IN,
    KEYWORD_EXISTS,
    KEYWORD_CASE,
    KEYWORD_WHEN,
    KEYWORD_THEN,
    KEYWORD_ELSE,
    KEYWORD_END,

    // 标识符和字面量
    IDENTIFIER,      // 表名、列名、函数名等
    STRING_LITERAL,  // 字符串字面量 'value'
    NUMBER_LITERAL,  // 数字字面量 123, 3.14
    BOOLEAN_LITERAL, // true, false

    // 符号
    LPAREN,          // (
    RPAREN,          // )
    COMMA,           // ,
    SEMICOLON,       // ;
    DOT,             // .

    // 运算符
    EQUAL,           // =
    NOT_EQUAL,       // != 或 <>
    LESS_THAN,       // <
    LESS_EQUAL,      // <=
    GREATER_THAN,    // >
    GREATER_EQUAL,   // >=

    // 特殊
    EOF,             // 文件结束
    UNKNOWN          // 未知 token
}
