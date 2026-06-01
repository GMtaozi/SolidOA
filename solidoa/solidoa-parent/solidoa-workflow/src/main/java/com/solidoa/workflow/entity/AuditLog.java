package com.solidoa.workflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作审计日志
 */
@Data
@TableName("oa_audit_log")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String action;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 操作描述 */
    private String description;

    /** 请求方法 */
    private String method;

    /** 请求路径 */
    private String requestPath;

    /** 请求参数 */
    private String requestParams;

    /** 响应状态 */
    private Integer responseStatus;

    /** IP地址 */
    private String ipAddress;

    /** 用户代理 */
    private String userAgent;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
