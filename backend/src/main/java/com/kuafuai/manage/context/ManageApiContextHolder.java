package com.kuafuai.manage.context;

/**
 * 管理API上下文持有者
 * 使用ThreadLocal存储当前请求的上下文信息
 */
public class ManageApiContextHolder {

    private static final ThreadLocal<ManageApiContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置上下文
     */
    public static void set(ManageApiContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取上下文
     */
    public static ManageApiContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 获取当前用户ID（场景1）
     */
    public static String getUserId() {
        ManageApiContext context = get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取企业ID（场景2）
     */
    public static String getCompanyId() {
        ManageApiContext context = get();
        return context != null ? context.getCompanyId() : null;
    }
}
