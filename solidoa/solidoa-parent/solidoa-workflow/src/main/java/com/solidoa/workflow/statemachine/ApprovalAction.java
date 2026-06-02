package com.solidoa.workflow.statemachine;

/**
 * 状态机动作接口（事件触发后执行的业务逻辑）
 */
@FunctionalInterface
public interface ApprovalAction {
    void execute(ApprovalContext ctx);
}
