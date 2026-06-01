package com.solidoa.workflow.service;

import com.solidoa.workflow.vo.ApprovalFlowVO;
import java.util.List;

/**
 * 审批节点服务接口
 */
public interface ApprovalNodeService {

    /**
     * 根据业务创建审批节点
     */
    void createNodes(String businessType, Long businessId, Long applicantId);

    /**
     * 获取审批流程详情
     */
    ApprovalFlowVO getApprovalFlow(String businessType, Long businessId);

    /**
     * 审批操作
     * @param approveType 审批类型，合法值: APPROVE(通过) / REJECT(驳回) / ADD_SIGN(加签) / TRANSFER(转交)
     */
    void approve(String businessType, Long businessId, Long approverId, String approveType, String comment);

    /**
     * 获取我的待审批数量
     */
    int countMyPending(Long userId);

    /**
     * 获取当前待审批节点
     */
    ApprovalFlowVO.CurrentNodeVO getCurrentNode(String businessType, Long businessId);
}
