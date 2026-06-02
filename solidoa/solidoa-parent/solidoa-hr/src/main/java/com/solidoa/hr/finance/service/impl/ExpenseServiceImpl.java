package com.solidoa.hr.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.constant.BizStatus;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.util.CryptoUtil;
import com.solidoa.hr.finance.entity.Expense;
import com.solidoa.hr.finance.entity.PaymentLog;
import com.solidoa.hr.finance.form.ExpenseForm;
import com.solidoa.hr.finance.mapper.ExpenseMapper;
import com.solidoa.hr.finance.mapper.PaymentLogMapper;
import com.solidoa.hr.finance.service.ExpenseService;
import com.solidoa.hr.finance.vo.ExpenseVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Autowired
    private com.solidoa.common.client.WorkflowClient workflowClient;

    @Override
    @Transactional
    public Long create(ExpenseForm form, Long userId, Long deptId) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报销金额必须大于0");
        }

        // 检查预算（带行锁防止并发超支）
        BudgetCheckResult checkResult = checkBudgetWithLock(deptId, form.getAmount());
        if (!checkResult.sufficient) {
            throw new BusinessException("预算不足，当前剩余：" + checkResult.remainingAmount + "，申请金额：" + form.getAmount());
        }

        Expense expense = new Expense();
        expense.setExpenseNo(generateExpenseNo());
        expense.setUserId(userId);
        expense.setDeptId(deptId);
        expense.setExpenseType(form.getExpenseType());
        expense.setAmount(form.getAmount());
        expense.setReason(form.getReason());
        expense.setAttachments(form.getAttachments());
        expense.setBankName(form.getBankName());
        // 银行账号使用AES-256-GCM加密存储，符合个人信息保护要求
        expense.setBankAccount(CryptoUtil.encrypt(form.getBankAccount()));
        expense.setStatus(BizStatus.EXPENSE_PENDING);
        expense.setCreateTime(LocalDateTime.now());

        String processInstanceId = "expense_" + System.currentTimeMillis();
        expense.setProcessInstanceId(processInstanceId);

        expenseMapper.insert(expense);
        // Sprint 3.4 修复：同步写审批节点
        syncApprovalNode("EXPENSE", expense.getId(), userId);
        log.info("创建报销单: id={}, no={}, amount={}", expense.getId(), expense.getExpenseNo(), form.getAmount());
        return expense.getId();
    }

    private void syncApprovalNode(String businessType, Long businessId, Long applicantId) {
        try {
            workflowClient.createApprovalNodes(businessType, businessId, applicantId);
            log.debug("审批节点同步成功: {}#{}", businessType, businessId);
        } catch (Exception e) {
            log.warn("审批节点同步失败（不影响主流程）: {}#{}, reason={}", businessType, businessId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void confirmPayment(Long id, Long cashierId, String confirmNote) {
        Expense expense = expenseMapper.selectById(id); // BaseMapper<Expense>
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        if (!BizStatus.EXPENSE_APPROVED.equals(expense.getStatus())) {
            throw new BusinessException("只有已审批通过的报销单才能付款");
        }

        // 更新状态
        expense.setStatus(BizStatus.COMPLETED);
        expense.setPaymentTime(LocalDateTime.now());
        expenseMapper.updateById(expense);

        // 记录付款日志
        PaymentLog paymentLog = new PaymentLog();
        paymentLog.setExpenseId(id);
        paymentLog.setCashierId(cashierId);
        paymentLog.setAmount(expense.getAmount());
        paymentLog.setConfirmNote(confirmNote);
        paymentLog.setPaymentTime(LocalDateTime.now());
        paymentLog.setCreateTime(LocalDateTime.now());
        paymentLogMapper.insert(paymentLog);

        log.info("报销单付款确认: id={}, cashierId={}, amount={}", id, cashierId, expense.getAmount());
    }

    @Override
    @Transactional
    public void approve(Long id, String approveResult, String comment, Long approverId) {
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }
        if (!BizStatus.EXPENSE_PENDING.equals(expense.getStatus())) {
            throw new BusinessException("只能审批待审批状态的报销单");
        }

        String newStatus = "APPROVE".equals(approveResult) ? BizStatus.EXPENSE_APPROVED : BizStatus.EXPENSE_REJECTED;
        expense.setStatus(newStatus);
        expense.setApproverId(approverId);
        expense.setApprovedTime(LocalDateTime.now());
        expense.setApproverComment(comment);
        expenseMapper.updateById(expense);
        log.info("审批报销单: id={}, result={}, approver={}", id, approveResult, approverId);
    }

    @Override
    @Transactional
    public void cancel(Long id, Long userId) {
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }
        if (!expense.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的报销单");
        }
        if (!BizStatus.EXPENSE_PENDING.equals(expense.getStatus())) {
            throw new BusinessException("只能撤回待审批状态的报销单");
        }

        expense.setStatus(BizStatus.EXPENSE_CANCELLED);
        expenseMapper.updateById(expense);
        log.info("撤回报销单: id={}, userId={}", id, userId);
    }

    @Override
    public PageVO<ExpenseVO> pageList(PageDTO dto, Long userId, String status) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<ExpenseVO> records = expenseMapper.selectPageList(offset, dto.getPageSize(), userId, status);
        long total = expenseMapper.selectCount(userId, status);

        PageVO<ExpenseVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public ExpenseVO getById(Long id) {
        return expenseMapper.selectVOById(id);
    }

    @Override
    public Map<String, Object> getStatistics(String startDate, String endDate) {
        List<ExpenseVO> expenses = expenseMapper.selectStatistics(startDate, endDate);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (ExpenseVO e : expenses) {
            if (e.getAmount() != null) {
                totalAmount = totalAmount.add(e.getAmount());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAmount", totalAmount);
        result.put("count", expenses.size());
        result.put("items", expenses);
        return result;
    }

    @Override
    public BigDecimal getMonthlyExpenseAmount(Long userId, String yearMonth) {
        LocalDate startDate = LocalDate.parse(yearMonth + "-01");
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        List<Expense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<Expense>()
                .eq(Expense::getUserId, userId)
                .ge(Expense::getCreateTime, startDate.atStartOfDay())
                .le(Expense::getCreateTime, endDate.atTime(23, 59, 59))
                .in(Expense::getStatus, Arrays.asList(BizStatus.EXPENSE_APPROVED, BizStatus.COMPLETED))
        );

        return expenses.stream()
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getYearlyExpenseAmount(Long userId, String year) {
        LocalDate startDate = LocalDate.parse(year + "-01-01");
        LocalDate endDate = LocalDate.parse(year + "-12-31");

        List<Expense> expenses = expenseMapper.selectList(
            new LambdaQueryWrapper<Expense>()
                .eq(Expense::getUserId, userId)
                .ge(Expense::getCreateTime, startDate.atStartOfDay())
                .le(Expense::getCreateTime, endDate.atTime(23, 59, 59))
                .in(Expense::getStatus, Arrays.asList(BizStatus.EXPENSE_APPROVED, BizStatus.COMPLETED))
        );

        return expenses.stream()
                .map(Expense::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 带行锁的预算检查
     * 使用 SELECT FOR UPDATE 防止并发超支
     */
    private BudgetCheckResult checkBudgetWithLock(Long deptId, BigDecimal amount) {
        LocalDate now = LocalDate.now();
        Integer year = now.getYear();
        Integer month = now.getMonthValue();

        // 查询预算（应用层锁，实际生产环境应在数据库层实现 FOR UPDATE）
        com.solidoa.hr.finance.entity.Budget budget = expenseMapper.selectBudgetForUpdate(deptId, year, month);

        if (budget == null) {
            // 无预算记录，默认不允许报销
            return new BudgetCheckResult(false, BigDecimal.ZERO);
        }

        BigDecimal remaining = budget.getRemainingAmount();
        if (remaining == null) {
            remaining = budget.getTotalAmount().subtract(budget.getUsedAmount() != null ? budget.getUsedAmount() : BigDecimal.ZERO);
        }

        boolean sufficient = remaining.compareTo(amount) >= 0;
        return new BudgetCheckResult(sufficient, remaining);
    }

    private String generateExpenseNo() {
        return "EXP" + System.currentTimeMillis();
    }

    private static class BudgetCheckResult {
        boolean sufficient;
        BigDecimal remainingAmount;

        BudgetCheckResult(boolean sufficient, BigDecimal remainingAmount) {
            this.sufficient = sufficient;
            this.remainingAmount = remainingAmount;
        }
    }
}