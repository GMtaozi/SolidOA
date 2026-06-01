package com.solidoa.workflow.service;

import com.solidoa.workflow.entity.ApprovalFlowConfig;
import com.solidoa.workflow.form.ApprovalFlowConfigForm;
import java.util.List;

/**
 * 审批流程配置服务接口
 */
public interface ApprovalFlowService {

    /**
     * 创建审批流程配置
     */
    Long createConfig(ApprovalFlowConfigForm form);

    /**
     * 更新审批流程配置
     */
    void updateConfig(Long id, ApprovalFlowConfigForm form);

    /**
     * 删除审批流程配置
     */
    void deleteConfig(Long id);

    /**
     * 获取业务类型的默认流程
     */
    ApprovalFlowConfig getDefaultConfig(String businessType);

    /**
     * 获取业务类型的流程配置列表
     */
    List<ApprovalFlowConfig> listByBusinessType(String businessType);

    /**
     * 设置默认流程
     */
    void setDefault(Long id, String businessType);
}
