package com.solidoa.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.workflow.entity.Leave;
import com.solidoa.workflow.entity.ApprovalRecord;
import com.solidoa.workflow.form.LeaveForm;
import com.solidoa.workflow.vo.LeaveVO;
import com.solidoa.common.vo.PageVO;
import com.solidoa.common.dto.PageDTO;
import com.solidoa.workflow.service.LeaveService;
import com.solidoa.workflow.service.ApprovalNodeService;
import com.solidoa.workflow.service.UniversalApprovalService;
import com.solidoa.workflow.enums.ApprovalEvent;
import com.solidoa.workflow.mapper.LeaveMapper;
import com.solidoa.workflow.mapper.ApprovalRecordMapper;
import com.solidoa.workflow.mq.WorkflowMessageProducer;
import com.solidoa.workflow.mq.ApprovalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private ApprovalRecordMapper approvalRecordMapper;

    @Autowired
    private WorkflowMessageProducer messageProducer;

    @Autowired
    private ApprovalNodeService approvalNodeService;

    @Autowired
    private UniversalApprovalService universalService;

    @Override
    @Transactional
    public Long createLeave(LeaveForm form, Long userId) {
        Leave leave = new Leave();
        leave.setLeaveNo(generateLeaveNo());
        leave.setUserId(userId);
        leave.setLeaveType(form.getLeaveType());
        leave.setStartDate(form.getStartDate());
        leave.setEndDate(form.getEndDate());
        if (form.getDays() == null && form.getStartDate() != null && form.getEndDate() != null) {
            // 兼容未传 days 字段的情况，根据日期自动计算
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(form.getStartDate(), form.getEndDate()) + 1;
            leave.setDays(java.math.BigDecimal.valueOf(daysBetween));
        } else {
            leave.setDays(form.getDays());
        }
        leave.setHours(form.getHours());
        leave.setReason(form.getReason());
        leave.setAttachments(form.getAttachments());
        leave.setStatus("PENDING");
        leave.setCreateTime(LocalDateTime.now());

        // TODO: 当前使用时间戳伪造 processInstanceId，集成 Camunda 后应改为 RuntimeService.startProcessInstanceByKey()
        String processInstanceId = "proc_" + System.currentTimeMillis();
        leave.setProcessInstanceId(processInstanceId);

        leaveMapper.insert(leave);
        log.info("创建请假申请: id={}, no={}", leave.getId(), leave.getLeaveNo());

        // 创建审批流程节点
        approvalNodeService.createNodes("LEAVE", leave.getId(), userId);

        return leave.getId();
    }

    @Override
    public PageVO<LeaveVO> pageList(PageDTO dto, Long userId, String status) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        List<LeaveVO> records = leaveMapper.selectPageList(offset, dto.getPageSize(), userId, status);
        long total = leaveMapper.selectCount(userId, status);

        PageVO<LeaveVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public LeaveVO getById(Long id, Long userId) {
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) {
            throw new BusinessException("请假单不存在");
        }
        // 权限校验：仅申请人本人或管理员可查看
        if (!leave.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该请假单");
        }
        return leaveMapper.selectVOById(id);
    }

    @Override
    public LeaveVO getByIdSimple(Long id) {
        // 简单的ID查询，无权限校验（供 Feign 调用）
        Leave leave = leaveMapper.selectById(id);
        if (leave == null) {
            return null;
        }
        return leaveMapper.selectVOById(id);
    }

    @Override
    @Transactional
    public void approve(Long id, String approveResult, String comment, Long approverId) {
        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        ApprovalEvent event = "APPROVED".equals(approveResult) ? ApprovalEvent.APPROVE : ApprovalEvent.REJECT;
        universalService.fire("LEAVE", id, approverId, event, comment);

        // 业务副作用：记录审批历史
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("LEAVE");
        record.setBusinessId(id);
        record.setApproverId(approverId);
        record.setApproveType(approveResult);
        record.setComment(comment);
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        // 业务副作用：发送消息通知
        Leave leave = leaveMapper.selectById(id);
        if (leave != null) {
            ApprovalMessage message = new ApprovalMessage();
            message.setBusinessType("LEAVE");
            message.setBusinessId(id);
            message.setApproverId(approverId);
            message.setActionType(approveResult);
            message.setComment(comment);
            message.setApplyUserId(leave.getUserId());
            message.setBusinessNo(leave.getLeaveNo());
            message.setCreateTime(LocalDateTime.now());
            messageProducer.sendApprovalMessage(message);
        }

        log.info("请假审批完成: id={}, result={}, approver={}", id, approveResult, approverId);
    }

    @Override
    @Transactional
    public void cancel(Long id, Long userId) {
        // [A1 第二步] 委托 UniversalApprovalService 走状态机 + 乐观锁
        universalService.fire("LEAVE", id, userId, ApprovalEvent.WITHDRAW, "申请人撤回");

        log.info("请假申请撤回: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void addSign(Long id, Long addUserId, Long currentApproverId) {
        // [A1 第二步] 循环审批检查保留（调用方）
        if (hasCircularReference("LEAVE", id, addUserId, currentApproverId)) {
            throw new BusinessException("检测到循环审批，禁止此操作");
        }

        // [A1 第二步] 委托 UniversalApprovalService：换审批人，不改状态
        universalService.transfer("LEAVE", id, currentApproverId, addUserId, null, "加签给用户: " + addUserId);

        // 业务副作用：记录加签历史
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("LEAVE");
        record.setBusinessId(id);
        record.setApproverId(currentApproverId);
        record.setApproveType("ADD_SIGN");
        record.setComment("加签给用户: " + addUserId);
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        log.info("请假申请加签: id={}, addUserId={}, from={}", id, addUserId, currentApproverId);
    }

    @Override
    @Transactional
    public void transfer(Long id, Long toUserId, String reason, Long currentApproverId) {
        // [A1 第二步] 循环审批检查保留（调用方）
        if (hasCircularReference("LEAVE", id, toUserId, currentApproverId)) {
            throw new BusinessException("检测到循环审批，禁止此操作");
        }

        // [A1 第二步] 委托 UniversalApprovalService：换审批人
        universalService.transfer("LEAVE", id, currentApproverId, toUserId, null, reason);

        // 业务副作用：记录转交历史
        ApprovalRecord record = new ApprovalRecord();
        record.setBusinessType("LEAVE");
        record.setBusinessId(id);
        record.setApproverId(currentApproverId);
        record.setApproveType("TRANSFER");
        record.setComment("转交给用户: " + toUserId + ", 原因: " + reason);
        record.setCreateTime(LocalDateTime.now());
        approvalRecordMapper.insert(record);

        log.info("请假申请转交: id={}, toUserId={}, from={}, reason={}", id, toUserId, currentApproverId, reason);
    }

    /**
     * 检查循环审批：检测 toUserId 是否在审批链中出现过
     */
    private boolean hasCircularReference(String businessType, Long businessId, Long toUserId, Long currentApproverId) {
        List<ApprovalRecord> history = approvalRecordMapper.selectByBusiness(businessType, businessId);

        // 如果目标用户是申请人，不允许
        Leave leave = leaveMapper.selectById(businessId);
        if (leave != null && toUserId.equals(leave.getUserId())) {
            return true;
        }

        // 收集审批历史中所有审批人ID
        Set<Long> approverIds = history.stream()
            .map(ApprovalRecord::getApproverId)
            .collect(Collectors.toSet());

        // 检查是否形成循环：如果目标用户已经在审批链中，且不是当前审批人自己，则形成循环
        if (approverIds.contains(toUserId) && !toUserId.equals(currentApproverId)) {
            return true;
        }

        return false;
    }

    private String generateLeaveNo() {
        String datePrefix = "LV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 使用行锁保证并发唯一性
        String maxNo = leaveMapper.selectMaxLeaveNoByDateForUpdate(datePrefix + "%");
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
}