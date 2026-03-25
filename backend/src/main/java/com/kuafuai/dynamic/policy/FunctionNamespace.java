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
     * 求值指定方法，返回强类型的值
     * <p>
     * 设计原则：
     * - 只负责求值，返回 Java 类型的值
     * - 不负责转换为 SQL 语法，由外层（PolicyExpressionParser）处理
     *
     * @param methodName 方法名（如 "uid", "user_id", "tenant_id"）
     * @return RlsValue 对象，包含类型和值
     */
    RlsValue evaluate(String methodName);
}
