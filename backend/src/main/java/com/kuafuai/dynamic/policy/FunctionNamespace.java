package com.kuafuai.dynamic.policy;

/**
 * RLS 函数命名空间接口
 * <p>
 * 将相关函数组织在同一个命名空间下，便于扩展和管理
 * 例如：auth.uid(), auth.role(), auth.email() 都属于 auth 命名空间
 */
public interface FunctionNamespace {

    /**
     * 获取命名空间名称
     *
     * @return 命名空间名称（如 "auth"）
     */
    String getName();

    /**
     * 求值指定方法，返回 SQL 字面量
     *
     * @param methodName 方法名（如 "uid", "role"）
     * @return SQL 字面量字符串（已包含引号，如 "'123'" 或 "NULL"）
     */
    String evaluate(String methodName);
}
