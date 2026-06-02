package com.solidoa.workflow.statemachine;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.enums.ApprovalState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 轻量级审批状态机门面
 *
 * 解决原 4 个业务 ServiceImpl 各自重复 if-else 的问题
 * 用一张 transition 表 + 守卫 + 动作的声明式配置替代散落判断
 *
 * 使用示例：
 * <pre>
 * ApprovalStateMachine sm = new ApprovalStateMachine()
 *     .when(ApprovalState.PENDING).on(ApprovalEvent.APPROVE)
 *         .guard(ctx -> ctx.isCurrentApprover())
 *         .action(ctx -> { ctx.advanceToNextNode(); })
 *         .to(ApprovalState.APPROVING)
 *     .when(ApprovalState.APPROVING).on(ApprovalEvent.APPROVE)
 *         .guard(ctx -> ctx.isLastNode())
 *         .action(ctx -> { ctx.markCompleted(); })
 *         .to(ApprovalState.APPROVED)
 *     .when(ApprovalState.PENDING).on(ApprovalEvent.WITHDRAW)
 *         .guard(ctx -> ctx.isInitiator())
 *         .to(ApprovalState.WITHDRAWN);
 *
 * ApprovalState newState = sm.fire(currentState, ApprovalEvent.APPROVE, ctx);
 * </pre>
 */
public class ApprovalStateMachine {

    /** transition 缓存：state -> (event -> Transition) */
    private final Map<ApprovalState, Map<ApprovalEvent, Transition>> transitions = new HashMap<>();

    /** 当前构建中的 source state（链式 API 支持） */
    private ApprovalState currentSource;

    /**
     * 开启一个 source state 的配置（链式起点）
     */
    public ApprovalStateMachine when(ApprovalState source) {
        this.currentSource = source;
        return this;
    }

    /**
     * 在当前 source state 上注册一个 event
     */
    public TransitionBuilder on(ApprovalEvent event) {
        return new TransitionBuilder(this, currentSource, event);
    }

    /**
     * 注册 transition（内部用）
     */
    void register(ApprovalState source, ApprovalEvent event, Transition transition) {
        transitions.computeIfAbsent(source, k -> new HashMap<>()).put(event, transition);
    }

    /**
     * 触发事件，返回新状态
     * @throws BusinessException 如果没有匹配的 transition 或守卫失败
     */
    public ApprovalState fire(ApprovalState current, ApprovalEvent event, ApprovalContext ctx) {
        Map<ApprovalEvent, Transition> map = transitions.get(current);
        if (map == null) {
            throw new BusinessException("当前状态 [" + current + "] 不支持任何事件");
        }
        Transition t = map.get(event);
        if (t == null) {
            throw new BusinessException("当前状态 [" + current + "] 不能触发事件 [" + event + "]");
        }
        // 1. 守卫
        if (t.guard != null && !t.guard.test(ctx)) {
            throw new BusinessException("事件 [" + event + "] 的前置条件未满足");
        }
        // 2. 动作
        if (t.action != null) {
            t.action.execute(ctx);
        }
        // 3. 返回新状态
        return t.target;
    }

    /**
     * 检查某状态+事件组合是否合法（不执行动作）
     */
    public boolean canFire(ApprovalState current, ApprovalEvent event) {
        Map<ApprovalEvent, Transition> map = transitions.get(current);
        return map != null && map.containsKey(event);
    }

    /**
     * Transition 定义
     */
    static class Transition {
        final ApprovalState target;
        final Predicate<ApprovalContext> guard;
        final ApprovalAction action;

        Transition(ApprovalState target, Predicate<ApprovalContext> guard, ApprovalAction action) {
            this.target = target;
            this.guard = guard;
            this.action = action;
        }
    }

    /**
     * 链式 builder：在 source+event 上绑定 target/guard/action
     */
    public static class TransitionBuilder {
        private final ApprovalStateMachine machine;
        private final ApprovalState source;
        private final ApprovalEvent event;
        private Predicate<ApprovalContext> guard;
        private ApprovalAction action;

        TransitionBuilder(ApprovalStateMachine machine, ApprovalState source, ApprovalEvent event) {
            this.machine = machine;
            this.source = source;
            this.event = event;
        }

        public TransitionBuilder guard(Predicate<ApprovalContext> guard) {
            this.guard = guard;
            return this;
        }

        public TransitionBuilder action(ApprovalAction action) {
            this.action = action;
            return this;
        }

        public ApprovalStateMachine to(ApprovalState target) {
            machine.register(source, event, new Transition(target, guard, action));
            return machine;
        }
    }
}
