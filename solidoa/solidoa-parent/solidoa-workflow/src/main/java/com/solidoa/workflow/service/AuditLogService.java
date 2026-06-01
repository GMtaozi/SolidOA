package com.solidoa.workflow.service;

import com.solidoa.workflow.entity.AuditLog;
import java.util.List;

/**
 * 审计日志服务接口
 */
public interface AuditLogService {

    /**
     * 记录审计日志
     */
    void log(Long userId, String username, String module, String action,
             String businessType, Long businessId, String description);

    /**
     * 查询用户的审计日志
     * @param page 页码（裸int参数，与MyBatis-Plus分页保持一致）
     * @param size 每页数量
     */
    List<AuditLog> listByUser(Long userId, int page, int size);

    /**
     * 查询业务的审计日志
     */
    List<AuditLog> listByBusiness(String businessType, Long businessId);
}
