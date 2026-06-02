package com.solidoa.workflow.enums;

/**
 * 审批事件枚举
 *
 * 与 ApprovalState 一一对应，状态机基于 Event 驱动 transition
 */
public enum ApprovalEvent {
    /** 提交（草稿→待审） */
    SUBMIT,
    /** 同意（待审/审批中→下一节点或已完成） */
    APPROVE,
    /** 拒绝（任意中间态→已拒绝） */
    REJECT,
    /** 撤回（申请人主动撤） */
    WITHDRAW,
    /** 转交（当前节点换人） */
    TRANSFER,
    /** 加签（在当前节点后追加审批人） */
    ADD_SIGN,
    /** 完成（含抄送节点已读） */
    COMPLETE,
    /** 超时（自动催办或跳过） */
    TIMEOUT
}
