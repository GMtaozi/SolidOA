package com.solidoa.common.security;

import com.solidoa.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 统一权限校验工具
 * 解决各 Service 中重复的权限校验逻辑
 */
@Slf4j
public class PermissionHelper {

    /**
     * 校验数据所属者
     * @param ownerId 数据拥有者ID
     * @param currentUserId 当前用户ID
     * @param resourceName 资源名称（如 "文件"、"请假单"）
     */
    public static void checkOwner(Long ownerId, Long currentUserId, String resourceName) {
        if (ownerId == null || currentUserId == null) {
            throw new BusinessException(403, "无法验证资源权限");
        }
        if (!ownerId.equals(currentUserId)) {
            log.warn("越权访问尝试: {} 资源, 拥有者={}, 访问者={}",
                resourceName, ownerId, currentUserId);
            throw new BusinessException(403, "无权限访问该" + resourceName);
        }
    }

    /**
     * 校验部门数据权限
     * @param dataDeptId 数据所属部门ID
     * @param currentUserDeptId 当前用户部门ID
     * @param isAdmin 当前用户是否为管理员
     */
    public static void checkDeptAccess(Long dataDeptId, Long currentUserDeptId, boolean isAdmin) {
        if (isAdmin) {
            return; // 管理员跳过
        }
        if (dataDeptId == null || currentUserDeptId == null) {
            throw new BusinessException(403, "无法验证部门权限");
        }
        if (!dataDeptId.equals(currentUserDeptId)) {
            log.warn("跨部门访问尝试: 数据部门={}, 用户部门={}", dataDeptId, currentUserDeptId);
            throw new BusinessException(403, "无权限访问其他部门数据");
        }
    }

    /**
     * 校验审批权限
     * @param approverId 审批人ID
     * @param currentUserId 当前用户ID
     */
    public static void checkApprovalRight(Long approverId, Long currentUserId) {
        if (approverId == null || currentUserId == null) {
            throw new BusinessException(403, "无法验证审批权限");
        }
        if (!approverId.equals(currentUserId)) {
            log.warn("越权审批尝试: 审批人={}, 操作者={}", approverId, currentUserId);
            throw new BusinessException(403, "您不是当前审批人");
        }
    }

    /**
     * 安全获取当前用户ID
     */
    public static Long getCurrentUserId() {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "用户未登录");
        }
        return userId;
    }

    /**
     * 安全获取当前用户部门ID
     */
    public static Long getCurrentDeptId() {
        return UserContextHolder.getDeptId();
    }

    /**
     * 检查是否为管理员
     */
    public static boolean isAdmin() {
        var context = UserContextHolder.get();
        if (context == null || context.getRoles() == null) {
            return false;
        }
        for (String role : context.getRoles().split(",")) {
            if ("ADMIN".equals(role.trim())) {
                return true;
            }
        }
        return false;
    }
}