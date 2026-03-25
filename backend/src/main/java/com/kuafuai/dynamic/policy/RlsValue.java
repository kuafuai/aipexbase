package com.kuafuai.dynamic.policy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * RLS 函数求值结果
 * 包含类型信息和实际值
 * <p>
 * 设计原则：
 * - auth 命名空间只负责求值，返回强类型的 RlsValue
 * - 外层（PolicyExpressionParser）负责转换为 SQL 字面量
 * - 职责清晰，解耦 SQL 语法
 */
@Getter
@ToString
@EqualsAndHashCode
public class RlsValue {

    /**
     * 值类型
     */
    private final RlsValueType type;

    /**
     * 实际值（Java 对象）
     * - STRING: String
     * - NUMBER: Long, Integer, Double 等
     * - BOOLEAN: Boolean
     * - NULL: null
     */
    private final Object value;

    private RlsValue(RlsValueType type, Object value) {
        this.type = type;
        this.value = value;
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建 NULL 值
     */
    public static RlsValue ofNull() {
        return new RlsValue(RlsValueType.NULL, null);
    }

    /**
     * 创建字符串值
     */
    public static RlsValue ofString(String value) {
        if (value == null) {
            return ofNull();
        }
        return new RlsValue(RlsValueType.STRING, value);
    }

    /**
     * 创建数字值
     */
    public static RlsValue ofNumber(Number value) {
        if (value == null) {
            return ofNull();
        }
        return new RlsValue(RlsValueType.NUMBER, value);
    }

    /**
     * 创建布尔值
     */
    public static RlsValue ofBoolean(Boolean value) {
        if (value == null) {
            return ofNull();
        }
        return new RlsValue(RlsValueType.BOOLEAN, value);
    }

    // ========== 辅助方法 ==========

    /**
     * 是否为 NULL
     */
    public boolean isNull() {
        return type == RlsValueType.NULL;
    }

    /**
     * 转换为 SQL 字面量
     * <p>
     * 示例：
     * - STRING: "admin" → "'admin'"
     * - NUMBER: 123 → "123"
     * - BOOLEAN: true → "true"
     * - NULL: null → "NULL"
     */
    public String toSqlLiteral() {
        switch (type) {
            case NULL:
                return "NULL";

            case STRING:
                // 字符串：加单引号并转义
                return "'" + escapeSql((String) value) + "'";

            case NUMBER:
            case BOOLEAN:
                // 数字和布尔：直接转字符串
                return String.valueOf(value);

            default:
                throw new IllegalStateException("未知的 RLS 值类型: " + type);
        }
    }

    /**
     * SQL 字符串转义
     * 将单引号转义为双单引号（SQL 标准）
     */
    private String escapeSql(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''");
    }

    /**
     * 获取 Java 类型的值（用于应用层求值）
     */
    public Object getJavaValue() {
        return value;
    }
}
