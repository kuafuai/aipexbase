package com.kuafuai.dynamic.policy;

/**
 * RLS 值类型枚举
 * 定义策略表达式中支持的数据类型
 */
public enum RlsValueType {
    /**
     * 字符串类型（如 'admin', 'user_123'）
     */
    STRING,

    /**
     * 数字类型（如 123, 45.67）
     */
    NUMBER,

    /**
     * 布尔类型（如 true, false）
     */
    BOOLEAN,

    /**
     * NULL 类型（表示空值）
     */
    NULL
}
