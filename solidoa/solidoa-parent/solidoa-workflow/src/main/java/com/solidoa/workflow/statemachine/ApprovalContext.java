package com.solidoa.workflow.statemachine;

/**
 * 审批上下文（状态机执行时传递的业务数据）
 */
public class ApprovalContext {
    /** 业务类型 */
    private String businessType;
    /** 业务单据 ID */
    private Long businessId;
    /** 当前用户 ID（触发事件的用户） */
    private Long currentUserId;
    /** 当前节点 ID */
    private Long currentNodeId;
    /** 业务实体状态（可变，Action 中更新） */
    private String currentStatus;
    /** 审批意见（可选） */
    private String comment;
    /** 业务扩展数据（用于 Action 间的状态传递） */
    private Object payload;

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String v) { this.businessType = v; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long v) { this.businessId = v; }
    public Long getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(Long v) { this.currentUserId = v; }
    public Long getCurrentNodeId() { return currentNodeId; }
    public void setCurrentNodeId(Long v) { this.currentNodeId = v; }
    public String getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(String v) { this.currentStatus = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
    public Object getPayload() { return payload; }
    public void setPayload(Object v) { this.payload = v; }

    /** 是否当前审批人（业务层判断） */
    public boolean isCurrentApprover() {
        // 由 Service 在调用 fire() 前通过 payload 传入
        Boolean flag = (payload instanceof Boolean) ? (Boolean) payload : null;
        return flag != null && flag;
    }

    /** 是否申请人（撤回守卫） */
    public boolean isInitiator() {
        Boolean flag = (payload instanceof Boolean) ? (Boolean) payload : null;
        return flag != null && flag;
    }

    /** 是否最后一个节点 */
    public boolean isLastNode() {
        Boolean flag = (payload instanceof Boolean) ? (Boolean) payload : null;
        return flag != null && flag;
    }
}
