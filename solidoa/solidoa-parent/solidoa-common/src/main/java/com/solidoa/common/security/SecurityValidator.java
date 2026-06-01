package com.solidoa.common.security;

import com.solidoa.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务间安全校验工具
 *
 * 安全策略：
 * 1. 外部请求（经过网关）直接放行
 * 2. 内部服务调用需重新校验权限
 * 3. 敏感操作必须验证操作人身份
 */
@Slf4j
public class SecurityValidator {

    /**
     * 校验当前请求是否为已验证的外部请求
     * 外部请求通过网关时已验证 JWT，此处不再重复校验
     */
    public static void requireExternalRequest() {
        if (!UserContextHolder.isExternalRequest()) {
            throw new BusinessException(403, "仅支持外部请求访问");
        }
    }

    /**
     * 校验当前请求是否为已验证的请求
     */
    public static void requireVerified() {
        if (!UserContextHolder.isVerified()) {
            throw new BusinessException(401, "请求未认证");
        }
    }

    /**
     * 校验当前用户是否为特定用户
     * @param userId 期望的用户ID
     */
    public static void requireUser(Long userId) {
        Long currentUserId = UserContextHolder.getUserId();
        if (currentUserId == null || !currentUserId.equals(userId)) {
            log.warn("越权访问尝试: 当前用户={}, 期望用户={}", currentUserId, userId);
            throw new BusinessException(403, "无权限访问该资源");
        }
    }

    /**
     * 校验当前用户是否为管理员
     */
    public static void requireAdmin() {
        Long currentUserId = UserContextHolder.getUserId();
        String roles = UserContextHolder.get() != null ? UserContextHolder.get().getRoles() : "";

        if (currentUserId == null || !hasRole(roles, "ADMIN")) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    private static boolean hasRole(String roles, String targetRole) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        for (String role : roles.split(",")) {
            if (targetRole.equals(role.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验当前用户是否为特定部门
     * @param deptId 期望的部门ID
     */
    public static void requireDept(Long deptId) {
        Long currentDeptId = UserContextHolder.getDeptId();
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            log.warn("跨部门访问尝试: 当前部门={}, 期望部门={}", currentDeptId, deptId);
            throw new BusinessException(403, "无权限访问其他部门数据");
        }
    }

    /**
     * 安全获取当前用户ID（未验证时返回null）
     */
    public static Long safeGetUserId() {
        return UserContextHolder.getUserId();
    }

    /**
     * 安全获取当前用户名（未验证时返回null）
     */
    public static String safeGetUsername() {
        return UserContextHolder.getUsername();
    }
}