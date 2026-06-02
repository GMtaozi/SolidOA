package com.solidoa.workflow.service.impl;

import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.entity.Expense;
import com.solidoa.workflow.entity.Leave;
import com.solidoa.workflow.entity.Purchase;
import com.solidoa.workflow.entity.Stamp;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.enums.ApprovalState;
import com.solidoa.workflow.mapper.ExpenseMapper;
import com.solidoa.workflow.mapper.LeaveMapper;
import com.solidoa.workflow.mapper.PurchaseMapper;
import com.solidoa.workflow.mapper.StampMapper;
import com.solidoa.workflow.statemachine.ApprovalContext;
import com.solidoa.workflow.statemachine.ApprovalStateMachine;
import com.solidoa.workflow.statemachine.ApprovalStateMachineConfig;
import com.solidoa.workflow.service.UniversalApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通用审批门面的实现
 * 把 ApprovalController 的 4 个私有方法迁过来
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UniversalApprovalServiceImpl implements UniversalApprovalService {

    private final LeaveMapper leaveMapper;
    private final StampMapper stampMapper;
    private final PurchaseMapper purchaseMapper;
    private final ExpenseMapper expenseMapper;

    @Override
    public ApprovalState fire(String businessType, Long businessId, Long userId,
                              ApprovalEvent event, String comment) {
        // 1. 取当前状态
        String currentStatus = getBusinessStatus(businessType, businessId);
        ApprovalState state = ApprovalState.of(currentStatus);
        if (state == null) {
            throw new BusinessException("业务状态异常: " + currentStatus);
        }
        if (state.isFinal()) {
            throw new BusinessException("当前状态 [" + state + "] 为终态，不可操作");
        }

        // 2. 构造上下文
        ApprovalContext ctx = new ApprovalContext();
        ctx.setBusinessType(businessType);
        ctx.setBusinessId(businessId);
        ctx.setCurrentUserId(userId);
        ctx.setComment(comment);
        // 简化：所有守卫统一传 true（业务层可重写）
        ctx.setPayload(Boolean.TRUE);

        // 3. 触发状态机
        ApprovalStateMachine sm = ApprovalStateMachineConfig.getInstance();
        if (!sm.canFire(state, event)) {
            throw new BusinessException("当前状态 [" + state + "] 不支持事件 [" + event + "]");
        }
        ApprovalState newState = sm.fire(state, event, ctx);

        // 4. 同步业务表
        updateBusinessStatus(businessType, businessId, newState.name());
        log.info("[UniversalApproval] 状态机触发: type={}, id={}, event={}, oldState={}, newState={}",
            businessType, businessId, event, state, newState);
        return newState;
    }

    @Override
    public ApprovalState transfer(String businessType, Long businessId, Long fromUserId,
                                   Long toUserId, String toUserName, String reason) {
        // transfer 不改状态（流程继续），仅换审批人
        ApprovalState currentState = fire(businessType, businessId, fromUserId,
                                          ApprovalEvent.TRANSFER, reason);
        updateBusinessApprover(businessType, businessId, toUserId, toUserName);
        return currentState;
    }

    @Override
    public String getBusinessStatus(String businessType, Long businessId) {
        return switch (businessType.toUpperCase()) {
            case "LEAVE" -> {
                Leave l = leaveMapper.selectById(businessId);
                yield l != null ? l.getStatus() : null;
            }
            case "STAMP" -> {
                Stamp s = stampMapper.selectById(businessId);
                yield s != null ? s.getStatus() : null;
            }
            case "PURCHASE" -> {
                Purchase p = purchaseMapper.selectById(businessId);
                yield p != null ? p.getStatus() : null;
            }
            case "EXPENSE" -> {
                Expense e = expenseMapper.selectById(businessId);
                yield e != null ? e.getStatus() : null;
            }
            default -> throw new BusinessException("不支持的业务类型: " + businessType);
        };
    }

    @Override
    public void updateBusinessStatus(String businessType, Long businessId, String newStatus) {
        // [A1 前置] 使用 updateById：MyBatis-Plus 检测到 @Version 字段后自动
        //  WHERE version = ? AND SET version = version + 1，等价于 4 业务手动 eq(version) 守卫
        int rows = switch (businessType.toUpperCase()) {
            case "LEAVE" -> {
                Leave l = leaveMapper.selectById(businessId);
                if (l == null) yield 0;
                l.setStatus(newStatus);
                yield leaveMapper.updateById(l);
            }
            case "STAMP" -> {
                Stamp s = stampMapper.selectById(businessId);
                if (s == null) yield 0;
                s.setStatus(newStatus);
                yield stampMapper.updateById(s);
            }
            case "PURCHASE" -> {
                Purchase p = purchaseMapper.selectById(businessId);
                if (p == null) yield 0;
                p.setStatus(newStatus);
                yield purchaseMapper.updateById(p);
            }
            case "EXPENSE" -> {
                Expense e = expenseMapper.selectById(businessId);
                if (e == null) yield 0;
                e.setStatus(newStatus);
                yield expenseMapper.updateById(e);
            }
            default -> throw new BusinessException("不支持的业务类型: " + businessType);
        };
        if (rows == 0) {
            throw new BusinessException("数据已被其他操作修改，请刷新后重试");
        }
    }

    @Override
    public void updateBusinessApprover(String businessType, Long businessId, Long toUserId, String toUserName) {
        // [A1 前置] 同样走 updateById，MyBatis-Plus 自动 + version
        switch (businessType.toUpperCase()) {
            case "LEAVE" -> {
                Leave l = leaveMapper.selectById(businessId);
                if (l != null) {
                    l.setCurrentApproverId(toUserId);
                    leaveMapper.updateById(l);
                }
            }
            case "STAMP" -> {
                Stamp s = stampMapper.selectById(businessId);
                if (s != null) {
                    s.setCurrentApproverId(toUserId);
                    stampMapper.updateById(s);
                }
            }
            case "PURCHASE" -> {
                Purchase p = purchaseMapper.selectById(businessId);
                if (p != null) {
                    p.setCurrentApproverId(toUserId);
                    purchaseMapper.updateById(p);
                }
            }
            case "EXPENSE" -> {
                Expense e = expenseMapper.selectById(businessId);
                if (e != null) {
                    e.setCurrentApproverId(toUserId);
                    expenseMapper.updateById(e);
                }
            }
            default -> throw new BusinessException("不支持的业务类型: " + businessType);
        }
    }
}
