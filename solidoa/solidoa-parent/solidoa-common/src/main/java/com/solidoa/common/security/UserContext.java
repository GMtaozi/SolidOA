package com.solidoa.common.security;

import lombok.Data;

/**
 * 用户身份上下文
 * 存储当前请求的用户信息
 */
@Data
public class UserContext {

    private Long userId;
    private String username;
    private Long deptId;
    private String roles;

    /**
     * 请求来源：EXTERNAL（外部请求，已过网关验证）/ INTERNAL（内部服务调用）
     */
    private String source;

    /**
     * 是否已通过安全验证
     */
    private boolean verified;

    /**
     * 创建内部服务调用上下文
     */
    public static UserContext internal() {
        UserContext context = new UserContext();
        context.setSource("INTERNAL");
        context.setVerified(false);
        return context;
    }

    /**
     * 创建外部请求上下文（网关已验证）
     */
    public static UserContext external(Long userId, String username, Long deptId, String roles) {
        UserContext context = new UserContext();
        context.setUserId(userId);
        context.setUsername(username);
        context.setDeptId(deptId);
        context.setRoles(roles);
        context.setSource("EXTERNAL");
        context.setVerified(true);
        return context;
    }

    public boolean isExternalRequest() {
        return "EXTERNAL".equals(source);
    }

    public boolean isInternalRequest() {
        return "INTERNAL".equals(source);
    }
}