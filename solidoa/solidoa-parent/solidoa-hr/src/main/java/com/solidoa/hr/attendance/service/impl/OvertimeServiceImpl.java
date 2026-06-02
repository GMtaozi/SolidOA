package com.solidoa.hr.attendance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.hr.attendance.entity.Overtime;
import com.solidoa.hr.attendance.entity.OvertimeBreak;
import com.solidoa.hr.attendance.form.ApproveForm;
import com.solidoa.hr.attendance.form.OvertimeForm;
import com.solidoa.hr.attendance.mapper.OvertimeMapper;
import com.solidoa.hr.attendance.mapper.OvertimeBreakMapper;
import com.solidoa.hr.attendance.service.OvertimeService;
import com.solidoa.hr.attendance.vo.OvertimeBalanceVO;
import com.solidoa.hr.attendance.vo.OvertimeVO;
import com.solidoa.common.enums.OvertimeType;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 加班服务实现
 */
@Service
@Slf4j
public class OvertimeServiceImpl implements OvertimeService {

    @Autowired
    private OvertimeMapper overtimeMapper;

    @Autowired
    private OvertimeBreakMapper overtimeBreakMapper;

    @Autowired
    private com.solidoa.common.client.WorkflowClient workflowClient;

    /** 调休有效期（月数） */
    private static final int BREAK_VALIDITY_MONTHS = 6;

    /** 最小加班单位（小时） */
    private static final BigDecimal MIN_OVERTIME_UNIT = new BigDecimal("0.5");

    /** 调休比例 */
    private static final BigDecimal BREAK_RATIO = new BigDecimal("1.0");

    @Override
    @Transactional
    public Long createOvertime(OvertimeForm form, Long userId) {
        // 校验加班类型
        OvertimeType overtimeType = OvertimeType.WORKDAY;
        if (form.getOvertimeType() != null) {
            try {
                overtimeType = OvertimeType.valueOf(form.getOvertimeType());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的加班类型: " + form.getOvertimeType());
            }
        }

        // 校验时间
        if (form.getStartTime().isAfter(form.getEndTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }

        // 计算加班时长（最小单位0.5小时）
        BigDecimal hours = calculateOvertimeHours(form.getStartTime(), form.getEndTime());
        if (hours.compareTo(MIN_OVERTIME_UNIT) < 0) {
            throw new BusinessException("加班时长不能少于0.5小时");
        }

        // 生成加班单号
        String overtimeNo = generateOvertimeNo();

        // 构建加班记录
        Overtime overtime = new Overtime();
        overtime.setOvertimeNo(overtimeNo);
        overtime.setUserId(userId);
        overtime.setOvertimeType(overtimeType.name());
        overtime.setStartTime(form.getStartTime());
        overtime.setEndTime(form.getEndTime());
        overtime.setHours(hours);
        overtime.setReason(form.getReason());
        overtime.setCompensationType(form.getCompensationType() != null
            ? form.getCompensationType() : "BREAK");
        overtime.setStatus("PENDING");
        overtime.setCreateTime(LocalDateTime.now());

        overtimeMapper.insert(overtime);
        // Sprint 3.4 修复：同步写审批节点
        syncApprovalNode("OVERTIME", overtime.getId(), userId);
        log.info("创建加班申请: userId={}, overtimeNo={}, hours={}", userId, overtimeNo, hours);

        return overtime.getId();
    }

    /**
     * 同步写审批节点（Feign 远程调 workflow-service）
     */
    private void syncApprovalNode(String businessType, Long businessId, Long applicantId) {
        try {
            workflowClient.createApprovalNodes(businessType, businessId, applicantId);
            log.debug("审批节点同步成功: {}#{}", businessType, businessId);
        } catch (Exception e) {
            log.warn("审批节点同步失败（不影响主流程）: {}#{}, reason={}", businessType, businessId, e.getMessage());
        }
    }

    @Override
    public PageVO<OvertimeVO> listOvertime(com.solidoa.common.dto.PageDTO dto, String status,
                                           String overtimeType, LocalDate startDate, LocalDate endDate) {
        int offset = (dto.getPageNum() - 1) * dto.getPageSize();
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        List<OvertimeVO> records = overtimeMapper.selectPageList(
            offset, dto.getPageSize(), null, status, overtimeType, startDateTime, endDateTime);
        long total = overtimeMapper.selectCount(null, status, overtimeType, startDateTime, endDateTime);

        PageVO<OvertimeVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(dto.getPageNum());
        page.setPageSize(dto.getPageSize());
        return page;
    }

    @Override
    public OvertimeVO getOvertimeById(Long id, Long userId) {
        // 使用MyBatis-Plus查询
        Overtime overtime = overtimeMapper.selectById(id);
        if (overtime == null) {
            throw new BusinessException("加班记录不存在");
        }
        // 权限校验：仅申请人本人或管理员可查看
        if (!overtime.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该加班记录");
        }

        OvertimeVO vo = new OvertimeVO();
        vo.setId(overtime.getId());
        vo.setOvertimeNo(overtime.getOvertimeNo());
        vo.setUserId(overtime.getUserId());
        vo.setOvertimeType(overtime.getOvertimeType());
        vo.setStartTime(overtime.getStartTime());
        vo.setEndTime(overtime.getEndTime());
        vo.setHours(overtime.getHours());
        vo.setReason(overtime.getReason());
        vo.setCompensationType(overtime.getCompensationType());
        vo.setStatus(overtime.getStatus());
        vo.setCurrentApproverId(overtime.getCurrentApproverId());
        vo.setApprovedHours(overtime.getApprovedHours());
        vo.setCreateTime(overtime.getCreateTime());
        return vo;
    }

    @Override
    @Transactional
    public void approveOvertime(Long id, ApproveForm form, Long approverId) {
        Overtime overtime = overtimeMapper.selectById(id);
        if (overtime == null) {
            throw new BusinessException("加班记录不存在");
        }

        if (!"PENDING".equals(overtime.getStatus())) {
            throw new BusinessException("只能审批待审批状态的加班申请");
        }

        // 解析审批类型
        String approveType = form.getApproveType();
        if ("REJECT".equals(approveType)) {
            overtime.setStatus("REJECTED");
            log.info("审批拒绝加班: id={}, approverId={}", id, approverId);
        } else {
            // 审批通过
            overtime.setStatus("APPROVED");
            overtime.setCurrentApproverId(approverId);

            // 设置审批通过的时长
            BigDecimal approvedHours = form.getApprovedHours() != null
                ? form.getApprovedHours() : overtime.getHours();
            overtime.setApprovedHours(approvedHours);

            // 如果是调休类型，创建调休记录
            if ("BREAK".equals(overtime.getCompensationType())) {
                createBreakRecord(overtime, approvedHours);
            }

            log.info("审批通过加班: id={}, approverId={}, approvedHours={}",
                id, approverId, approvedHours);
        }

        overtimeMapper.updateById(overtime);
    }

    @Override
    @Transactional
    public void cancelOvertime(Long id, Long userId) {
        Overtime overtime = overtimeMapper.selectById(id);
        if (overtime == null) {
            throw new BusinessException("加班记录不存在");
        }

        if (!overtime.getUserId().equals(userId)) {
            throw new BusinessException("只能撤回自己的加班申请");
        }

        if (!"PENDING".equals(overtime.getStatus())) {
            throw new BusinessException("只能撤回待审批状态的加班申请");
        }

        overtime.setStatus("CANCELLED");
        overtimeMapper.updateById(overtime);
        log.info("撤回加班申请: id={}, userId={}", id, userId);
    }

    @Override
    public OvertimeBalanceVO getOvertimeBalance(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 查询可用调休时长
        BigDecimal available = overtimeMapper.selectAvailableBreakHours(userId, now);
        if (available == null) {
            available = BigDecimal.ZERO;
        }

        // 查询已使用调休时长
        BigDecimal used = overtimeMapper.selectUsedBreakHours(userId);
        if (used == null) {
            used = BigDecimal.ZERO;
        }

        // 查询调休记录数
        List<OvertimeBreak> breaks = overtimeMapper.selectValidBreakByUserId(userId, now);

        OvertimeBalanceVO vo = new OvertimeBalanceVO();
        vo.setUserId(userId);
        vo.setTotalAvailable(available);
        vo.setTotalUsed(used);
        vo.setBalance(available.subtract(used));
        vo.setBreakCount(breaks.size());

        return vo;
    }

    /**
     * 计算加班时长（最小单位0.5小时，向上取整）
     */
    private BigDecimal calculateOvertimeHours(LocalDateTime startTime, LocalDateTime endTime) {
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(totalMinutes)
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        // 向上取整到0.5小时（使用 CEILING 模式）
        hours = hours.multiply(BigDecimal.valueOf(2))
            .setScale(0, RoundingMode.CEILING)
            .divide(BigDecimal.valueOf(2), 1, RoundingMode.CEILING);
        return hours;
    }

    /**
     * 生成加班单号
     */
    private String generateOvertimeNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "OT" + timestamp + uuid;
    }

    /**
     * 创建调休记录（有效期6个月）
     */
    private void createBreakRecord(Overtime overtime, BigDecimal approvedHours) {
        OvertimeBreak breakRecord = new OvertimeBreak();
        breakRecord.setUserId(overtime.getUserId());
        breakRecord.setOvertimeId(overtime.getId());
        // 调休比例1:1
        breakRecord.setAvailableHours(approvedHours.multiply(BREAK_RATIO));
        breakRecord.setUsedHours(BigDecimal.ZERO);
        // 计算过期时间（6个月后）
        breakRecord.setExpiredTime(LocalDateTime.now().plusMonths(BREAK_VALIDITY_MONTHS));
        breakRecord.setCreateTime(LocalDateTime.now());

        overtimeBreakMapper.insert(breakRecord);
        log.info("创建调休记录: userId={}, overtimeId={}, availableHours={}, expiredTime={}",
            breakRecord.getUserId(), breakRecord.getOvertimeId(),
            breakRecord.getAvailableHours(), breakRecord.getExpiredTime());
    }
}