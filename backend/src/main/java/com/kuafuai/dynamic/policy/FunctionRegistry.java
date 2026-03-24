package com.kuafuai.dynamic.policy;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * RLS 函数命名空间注册中心
 * <p>
 * 采用命名空间模式组织函数，便于扩展
 * 例如：auth.uid(), auth.tenant_id() 都属于 auth 命名空间
 */
@Slf4j
public class FunctionRegistry {

    private static final Map<String, FunctionNamespace> NAMESPACES = new HashMap<>();

    static {
        // 注册命名空间
        register(new AuthNamespace());

        log.info("RLS 函数命名空间注册完成: {}", NAMESPACES.keySet());
    }

    /**
     * 注册命名空间
     */
    public static void register(FunctionNamespace namespace) {
        NAMESPACES.put(namespace.getName(), namespace);
        log.debug("注册 RLS 命名空间: {}", namespace.getName());
    }

    /**
     * 获取命名空间
     */
    public static FunctionNamespace getNamespace(String name) {
        return NAMESPACES.get(name);
    }

    /**
     * 判断是否是已注册的命名空间
     */
    public static boolean hasNamespace(String name) {
        return NAMESPACES.containsKey(name);
    }

    /**
     * 求值函数
     *
     * @param namespace 命名空间名称（如 "auth"）
     * @param method    方法名称（如 "uid"）
     * @return SQL 字面量
     */
    public static String evaluateFunction(String namespace, String method) {
        FunctionNamespace ns = NAMESPACES.get(namespace);
        if (ns == null) {
            log.warn("未找到命名空间: {}", namespace);
            return "NULL";
        }

        try {
            return ns.evaluate(method);
        } catch (Exception e) {
            log.error("函数求值失败: {}.{}()", namespace, method, e);
            return "NULL";
        }
    }
}
