package com.solidoa.workflow.enums;

/**
 * 审批状态枚举（主单据层）
 *
 * 设计原则：
 * - 与 SSD V2.0 第 6 章保持一致
 * - 9 个状态覆盖：草稿/待审/审批中/已通过/已拒绝/已撤回/抄送/加签中/已完成
 * - 兼容老数据 CANCELLED（=WITHDRAWN）
 */
public enum ApprovalState {
    /** 草稿（未提交） */
    DRAFT,
    /** 待审批（首节点 PENDING） */
    PENDING,
    /** 审批中（中间节点在进行中） */
    APPROVING,
    /** 加签中（当前节点存在加签子节点） */
    ADD_SIGNING,
    /** 已通过（终态） */
    APPROVED,
    /** 已拒绝（终态） */
    REJECTED,
    /** 已撤回（申请人主动撤，终态） */
    WITHDRAWN,
    /** 抄送中（仅抄送节点） */
    CC,
    /** 已完成（含抄送完毕的终态） */
    FINISHED;

    /**
     * 兼容老数据：原 CANCELLED 映射为 WITHDRAWN
     */
    public static ApprovalState of(String code) {
        if (code == null) return null;
        if ("CANCELLED".equals(code)) return WITHDRAWN;
        try {
            return valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 是否终态
     */
    public boolean isFinal() {
        return this == APPROVED || this == REJECTED || this == WITHDRAWN || this == FINISHED;
    }

    /**
     * 是否可审批（中间态）
     */
    public boolean isApprovable() {
        return this == PENDING || this == APPROVING || this == ADD_SIGNING;
    }
}
