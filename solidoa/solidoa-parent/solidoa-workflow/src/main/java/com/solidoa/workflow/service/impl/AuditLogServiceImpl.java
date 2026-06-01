package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.workflow.entity.AuditLog;
import com.solidoa.workflow.mapper.AuditLogMapper;
import com.solidoa.workflow.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 审计日志服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    /** 敏感字段名称（不区分大小写匹配） */
    private static final Set<String> SENSITIVE_FIELDS = new HashSet<>();
    static {
        SENSITIVE_FIELDS.add("password");
        SENSITIVE_FIELDS.add("pwd");
        SENSITIVE_FIELDS.add("secret");
        SENSITIVE_FIELDS.add("token");
        SENSITIVE_FIELDS.add("apikey");
        SENSITIVE_FIELDS.add("api_key");
        SENSITIVE_FIELDS.add("accesskey");
        SENSITIVE_FIELDS.add("access_key");
        SENSITIVE_FIELDS.add("privatekey");
        SENSITIVE_FIELDS.add("private_key");
        SENSITIVE_FIELDS.add("bankaccount");
        SENSITIVE_FIELDS.add("bank_account");
        SENSITIVE_FIELDS.add("cardno");
        SENSITIVE_FIELDS.add("card_no");
        SENSITIVE_FIELDS.add("idcard");
        SENSITIVE_FIELDS.add("id_card");
        SENSITIVE_FIELDS.add("ssn");
    }

    /** JSON 对象匹配模式 */
    private static final Pattern JSON_PATTERN = Pattern.compile("\\{[^{}]*\"[^\"]+\"[^{}]*:[^{}]*\\}");

    @Override
    @Async
    public void log(Long userId, String username, String module, String action,
                    String businessType, Long businessId, String description) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setModule(module);
            auditLog.setAction(action);
            auditLog.setBusinessType(businessType);
            auditLog.setBusinessId(businessId);
            auditLog.setDescription(description);
            auditLog.setCreateTime(java.time.LocalDateTime.now());

            auditLogMapper.insert(auditLog);
            log.debug("审计日志记录成功: userId={}, action={}, business={}:{}",
                    userId, action, businessType, businessId);
        } catch (Exception e) {
            // 审计日志失败不应影响主业务
            log.error("审计日志记录失败: userId={}, action={}, error={}",
                    userId, action, e.getMessage());
        }
    }

    /**
     * 敏感信息脱敏
     * 对 JSON 字符串中的敏感字段值进行掩码处理
     */
    private String sanitizeSensitiveData(String rawParams) {
        if (rawParams == null || rawParams.isEmpty()) {
            return rawParams;
        }

        String sanitized = rawParams;
        for (String field : SENSITIVE_FIELDS) {
            // 匹配 "field": "value" 或 "field":"value" 模式，替换 value 为掩码
            String pattern = "\"" + field + "\"\\s*:\\s*\"[^\"]*\"";
            sanitized = sanitized.replaceAll(pattern, "\"" + field + "\":\"******\"");
            // 匹配 "field": value 模式（数字），替换 value 为掩码
            String numPattern = "\"" + field + "\"\\s*:\\s*\\d+";
            sanitized = sanitized.replaceAll(numPattern, "\"" + field + "\":\"******\"");
        }
        return sanitized;
    }

    @Override
    public List<AuditLog> listByUser(Long userId, int page, int size) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, AuditLog::getUserId, userId);
        wrapper.orderByDesc(AuditLog::getCreateTime);

        Page<AuditLog> pageParam = new Page<>(page, size);
        IPage<AuditLog> result = auditLogMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public List<AuditLog> listByBusiness(String businessType, Long businessId) {
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(businessType != null, AuditLog::getBusinessType, businessType);
        wrapper.eq(businessId != null, AuditLog::getBusinessId, businessId);
        wrapper.orderByDesc(AuditLog::getCreateTime);

        return auditLogMapper.selectList(wrapper);
    }
}
