package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.entity.Expense;
import com.solidoa.workflow.entity.ApprovalRecord;
import com.solidoa.workflow.form.ExpenseForm;
import com.solidoa.workflow.mapper.ExpenseMapper;
import com.solidoa.workflow.mapper.ApprovalRecordMapper;
import com.solidoa.workflow.service.ExpenseService;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.vo.ExpenseVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.workflow.mq.WorkflowMessageProducer;
import com.solidoa.workflow.mq.ApprovalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private WorkflowMessageProducer messageProducer;

    @Autowired
    private ApprovalNodeService approvalNodeService;

    @Override
    @Transactional
    public Long createExpense(ExpenseForm form, Long userId) {
        Expense expense = new Expense();
        BeanUtils.copyProperties(form, expense);
        expense.setUserId(userId);
        expense.setExpenseNo(generateExpenseNo());
        expense.setStatus("PENDING");

        expenseMapper.insert(expense);
        log.info("创建报销单: {}", expense.getExpenseNo());

        // 创建审批流程节点
        approvalNodeService.createNodes("EXPENSE", expense.getId(), userId);

        return expense.getId();
    }

    @Override
    public PageVO<ExpenseVO> pageList(Long userId, String status, PageDTO dto) {
        int pageNum = dto.getPageNum();
        int pageSize = dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        List<ExpenseVO> records = expenseMapper.selectPageList(userId, status, offset, pageSize);
        long total = expenseMapper.selectCount(userId, status);

        PageVO<ExpenseVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }

    @Override
    public ExpenseVO getById(Long id) {
        ExpenseVO vo = new ExpenseVO();
        Expense expense = expenseMapper.selectExpenseById(id);
        if (expense != null) {
            BeanUtils.copyProperties(expense, vo);
        }
        return vo;
    }

    @Override
    @Transactional
    public void approve(Long id, String approveResult, String comment, Long approverId) {
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        if (!"PENDING".equals(expense.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        // 审批人身份校验：必须是当前审批人或申请人
        if (expense.getCurrentApproverId() == null || !expense.getCurrentApproverId().equals(approverId)) {
            throw new BusinessException("您不是该申请的当前审批人，无权审批");
        }

        String newStatus = "APPROVED".equals(approveResult) ? "APPROVED" : "REJECTED";
        expense.setStatus(newStatus);

        int rows = expenseMapper.update(
            expense,
            new LambdaQueryWrapper<Expense>()
                .eq(Expense::getId, id)
                .eq(Expense::getStatus, "PENDING")
                .eq(Expense::getVersion, expense.getVersion())
        );

        if (rows == 0) {
            throw new BusinessException("数据已被其他操作修改，请刷新后重试");
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("EXPENSE");
        record.setBusinessId(id);
        record.setApproverId(approverId);
        record.setApproveType(approveResult);
        record.setComment(comment);
        record.setCreateTime(java.time.LocalDateTime.now());
        approvalRecordMapper.insert(record);

        ApprovalMessage message = new ApprovalMessage();
        message.setBusinessType("EXPENSE");
        message.setBusinessId(id);
        message.setApproverId(approverId);
        message.setActionType(approveResult);
        message.setComment(comment);
        message.setApplyUserId(expense.getUserId());
        message.setBusinessNo(expense.getExpenseNo());
        message.setCreateTime(java.time.LocalDateTime.now());
        messageProducer.sendApprovalMessage(message);

        log.info("审批报销单: id={}, result={}, approver={}", id, approveResult, approverId);
    }

    @Override
    @Transactional
    public void addSign(Long id, Long addUserId, Long currentApproverId) {
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        // 检查循环审批：A -> B -> A
        if (hasCircularReference("EXPENSE", id, addUserId, currentApproverId)) {
            throw new BusinessException("检测到循环审批，禁止此操作");
        }

        expense.setCurrentApproverId(addUserId);
        int rows = expenseMapper.update(
            expense,
            new LambdaQueryWrapper<Expense>()
                .eq(Expense::getId, id)
                .eq(Expense::getStatus, "PENDING")
                .eq(Expense::getVersion, expense.getVersion())
        );

        if (rows == 0) {
            throw new BusinessException("数据已被其他操作修改，请刷新后重试");
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("EXPENSE");
        record.setBusinessId(id);
        record.setApproverId(currentApproverId);
        record.setApproveType("ADD_SIGN");
        record.setComment("加签给用户: " + addUserId);
        record.setCreateTime(java.time.LocalDateTime.now());
        approvalRecordMapper.insert(record);

        log.info("报销单加签: id={}, addUserId={}", id, addUserId);
    }

    @Override
    @Transactional
    public void transfer(Long id, Long toUserId, String reason, Long currentApproverId) {
        Expense expense = expenseMapper.selectById(id);
        if (expense == null) {
            throw new BusinessException("报销单不存在");
        }

        // 检查循环审批
        if (hasCircularReference("EXPENSE", id, toUserId, currentApproverId)) {
            throw new BusinessException("检测到循环审批，禁止此操作");
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("EXPENSE");
        record.setBusinessId(id);
        record.setApproverId(currentApproverId);
        record.setApproveType("TRANSFER");
        record.setComment("转交给用户: " + toUserId + ", 原因: " + reason);
        record.setCreateTime(java.time.LocalDateTime.now());
        approvalRecordMapper.insert(record);

        expense.setCurrentApproverId(toUserId);
        int rows = expenseMapper.update(
            expense,
            new LambdaQueryWrapper<Expense>()
                .eq(Expense::getId, id)
                .eq(Expense::getStatus, "PENDING")
                .eq(Expense::getVersion, expense.getVersion())
        );

        if (rows == 0) {
            throw new BusinessException("数据已被其他操作修改，请刷新后重试");
        }

        log.info("报销单转交: id={}, toUserId={}", id, toUserId);
    }

    private String generateExpenseNo() {
        String datePrefix = "EXP" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用行锁保证并发唯一性
        String maxNo = expenseMapper.selectMaxExpenseNoByDateForUpdate(datePrefix + "%");
        int seq = 1;
        if (maxNo != null && maxNo.length() > datePrefix.length()) {
            try {
                seq = Integer.parseInt(maxNo.substring(datePrefix.length())) + 1;
            } catch (NumberFormatException e) {
                seq = 1;
            }
        }
        return datePrefix + String.format("%04d", seq);
    }

    /**
     * 检查循环审批：检测 toUserId 是否在审批链中出现过
     */
    private boolean hasCircularReference(String businessType, Long businessId, Long toUserId, Long currentApproverId) {
        List<ApprovalRecord> history = approvalRecordMapper.selectByBusiness(businessType, businessId);

        // 如果目标用户是申请人，不允许
        Expense expense = expenseMapper.selectById(businessId);
        if (expense != null && toUserId.equals(expense.getUserId())) {
            return true;
        }

        // 收集审批历史中所有审批人ID
        Set<Long> approverIds = history.stream()
            .map(ApprovalRecord::getApproverId)
            .collect(Collectors.toSet());

        // 检查是否形成循环：如果目标用户已经在审批链中，且不是当前审批人自己，则形成循环
        return approverIds.contains(toUserId) && !toUserId.equals(currentApproverId);
    }
}