package com.solidoa.workflow.service;

import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.enums.ApprovalState;

/**
 * 通用审批门面（Sprint 4.1 - A1）
 *
 * 解决原 4 业务 ServiceImpl（Leave/Expense/Stamp/Purchase）各自重复实现
 * 4 遍 if-else 守卫 + setStatus + 乐观锁的问题
 *
 * 任何业务（4 业务 + hr 端 5 业务）的审批操作都应通过此 Service 进行，
 * 内部走 ApprovalStateMachine 统一守卫 + 状态切换。
 */
public interface UniversalApprovalService {

    /**
     * 触发审批事件（统一入口）
     * @param businessType LEAVE/EXPENSE/STAMP/PURCHASE/...
     * @param businessId 业务单据 ID
     * @param userId 当前操作人 ID
     * @param event 事件（APPROVE/REJECT/WITHDRAW/TRANSFER/ADD_SIGN）
     * @param comment 审批意见
     * @return 新状态
     */
    ApprovalState fire(String businessType, Long businessId, Long userId,
                       ApprovalEvent event, String comment);

    /**
     * 触发转交（带目标人）
     */
    ApprovalState transfer(String businessType, Long businessId, Long fromUserId,
                           Long toUserId, String toUserName, String reason);

    /**
     * 取业务表当前状态
     */
    String getBusinessStatus(String businessType, Long businessId);

    /**
     * 更新业务表 status（不直接调用，仅 fire() 内部使用）
     */
    void updateBusinessStatus(String businessType, Long businessId, String newStatus);

    /**
     * 更新业务表 currentApproverId（转交）
     */
    void updateBusinessApprover(String businessType, Long businessId, Long toUserId, String toUserName);
}
