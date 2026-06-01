package com.solidoa.common.security;

/**
 * 用户上下文持有者
 * ThreadLocal 存储当前线程的用户信息
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    public static UserContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 执行带用户上下文的操作，确保清理
     */
    public static <T> T executeWithContext(UserContext context, java.util.function.Supplier<T> action) {
        set(context);
        try {
            return action.get();
        } finally {
            clear();
        }
    }

    /**
     * 执行不带上下文的操作
     */
    public static <T> T executeWithoutContext(java.util.function.Supplier<T> action) {
        UserContext original = get();
        try {
            clear();
            return action.get();
        } finally {
            if (original != null) {
                set(original);
            }
        }
    }

    /**
     * 获取当前用户ID
     */
    public static Long getUserId() {
        UserContext context = get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        UserContext context = get();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取当前部门ID
     */
    public static Long getDeptId() {
        UserContext context = get();
        return context != null ? context.getDeptId() : null;
    }

    /**
     * 获取当前用户角色
     */
    public static String getRoles() {
        UserContext context = get();
        return context != null ? context.getRoles() : null;
    }

    /**
     * 获取当前用户角色列表
     */
    public static java.util.List<String> getRolesList() {
        String roles = getRoles();
        if (roles == null || roles.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return java.util.Arrays.asList(roles.split(","));
    }

    /**
     * 判断是否为外部请求（已通过网关验证）
     */
    public static boolean isExternalRequest() {
        UserContext context = get();
        return context != null && context.isExternalRequest();
    }

    /**
     * 判断是否为已验证的请求
     */
    public static boolean isVerified() {
        UserContext context = get();
        return context != null && context.isVerified();
    }
}