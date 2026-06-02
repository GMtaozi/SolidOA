package com.solidoa.workflow.statemachine;

import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.enums.ApprovalState;

/**
 * 审批状态机配置中心（单例）
 *
 * 集中定义所有 transition，避免散落在各业务 Service 中
 */
public final class ApprovalStateMachineConfig {

    private static volatile ApprovalStateMachine INSTANCE;

    private ApprovalStateMachineConfig() {}

    public static ApprovalStateMachine getInstance() {
        if (INSTANCE == null) {
            synchronized (ApprovalStateMachineConfig.class) {
                if (INSTANCE == null) {
                    INSTANCE = build();
                }
            }
        }
        return INSTANCE;
    }

    private static ApprovalStateMachine build() {
        ApprovalStateMachine sm = new ApprovalStateMachine();

        // ========== 草稿 → 待审 ==========
        sm.when(ApprovalState.DRAFT).on(ApprovalEvent.SUBMIT)
                .to(ApprovalState.PENDING);

        // ========== 待审 → 审批中（首个审批人开始处理） ==========
        sm.when(ApprovalState.PENDING).on(ApprovalEvent.APPROVE)
                .guard(ctx -> ctx.isCurrentApprover())
                .action(ctx -> { /* 推进到下一节点/或保留当前节点 */ })
                .to(ApprovalState.APPROVING);

        // ========== 审批中 → 已通过（最后一个节点通过） ==========
        sm.when(ApprovalState.APPROVING).on(ApprovalEvent.APPROVE)
                .guard(ctx -> ctx.isLastNode())
                .to(ApprovalState.APPROVED);

        // ========== 审批中 → 待审（中间节点通过，流转到下一节点） ==========
        sm.when(ApprovalState.APPROVING).on(ApprovalEvent.APPROVE)
                .guard(ctx -> !ctx.isLastNode())
                .to(ApprovalState.PENDING);

        // ========== 待审/审批中 → 已拒绝（任意中间态拒绝） ==========
        sm.when(ApprovalState.PENDING).on(ApprovalEvent.REJECT)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.REJECTED);
        sm.when(ApprovalState.APPROVING).on(ApprovalEvent.REJECT)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.REJECTED);

        // ========== 待审 → 加签中（加签触发） ==========
        sm.when(ApprovalState.PENDING).on(ApprovalEvent.ADD_SIGN)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.ADD_SIGNING);

        // ========== 加签中 → 待审（加签人处理完成） ==========
        sm.when(ApprovalState.ADD_SIGNING).on(ApprovalEvent.APPROVE)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.PENDING);
        sm.when(ApprovalState.ADD_SIGNING).on(ApprovalEvent.REJECT)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.REJECTED);

        // ========== 待审/审批中/加签中 → 已撤回（申请人主动撤） ==========
        sm.when(ApprovalState.PENDING).on(ApprovalEvent.WITHDRAW)
                .guard(ctx -> ctx.isInitiator())
                .to(ApprovalState.WITHDRAWN);
        sm.when(ApprovalState.APPROVING).on(ApprovalEvent.WITHDRAW)
                .guard(ctx -> ctx.isInitiator())
                .to(ApprovalState.WITHDRAWN);
        sm.when(ApprovalState.ADD_SIGNING).on(ApprovalEvent.WITHDRAW)
                .guard(ctx -> ctx.isInitiator())
                .to(ApprovalState.WITHDRAWN);

        // ========== 转交（不改变状态，只换审批人） ==========
        sm.when(ApprovalState.PENDING).on(ApprovalEvent.TRANSFER)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.PENDING);  // 状态不变
        sm.when(ApprovalState.APPROVING).on(ApprovalEvent.TRANSFER)
                .guard(ctx -> ctx.isCurrentApprover())
                .to(ApprovalState.APPROVING); // 状态不变

        // ========== 已通过 → 已完成（终态 + 抄送完毕） ==========
        sm.when(ApprovalState.APPROVED).on(ApprovalEvent.COMPLETE)
                .to(ApprovalState.FINISHED);

        return sm;
    }
}
